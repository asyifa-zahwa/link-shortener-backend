package com.example.linkshortener.service;

import com.example.linkshortener.dto.*;
import com.example.linkshortener.entity.ClickAnalytics;
import com.example.linkshortener.entity.Url;
import com.example.linkshortener.entity.User;
import com.example.linkshortener.repository.ClickAnalyticsRepository;
import com.example.linkshortener.repository.UrlRepository;
import com.example.linkshortener.repository.UserRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UrlService {

    private final UrlRepository urlRepository;
    private final Base62Service base62Service;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ClickAnalyticsRepository clickAnalyticsRepository; // 1. Suntikkan ini

    // 2. Sesuaikan Constructor kamu
    public UrlService(UrlRepository urlRepository, Base62Service base62Service,
                      UserRepository userRepository, RedisTemplate<String, Object> redisTemplate,
                      ClickAnalyticsRepository clickAnalyticsRepository) {
        this.urlRepository = urlRepository;
        this.base62Service = base62Service;
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
        this.clickAnalyticsRepository = clickAnalyticsRepository;
    }

    /**
     * Mengambil statistik detail analitik dari short code tertentu khusus untuk pemiliknya
     */
    @Transactional(readOnly = true)
    public ShortUrlAnalyticsResponse getUrlAnalytics(String shortCode, String currentUsername) {
        // Proteksi A: Wajib Login
        if (currentUsername == null || currentUsername.equals("anonymousUser")) {
            throw new SecurityException("Akses ditolak! Anda harus login untuk melihat analitik.");
        }

        // Ambil data URL
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new IllegalArgumentException("Link pendek tidak ditemukan!"));

        // Proteksi B: Validasi Kepemilikan Link
        if (url.getUser() == null || !url.getUser().getUsername().equals(currentUsername)) {
            throw new org.springframework.security.access.AccessDeniedException("Akses ditolak! Anda tidak memiliki hak untuk melihat analitik link ini.");
        }

        Long urlId = url.getId();

        // 1. Tarik total clicks global
        long totalClicks = clickAnalyticsRepository.countByUrlId(urlId);

        // 2. Tarik data Device Tuple dan ubah menjadi Map<String, Long>
        List<ClickCountTuple> deviceTuples = clickAnalyticsRepository.countClicksByDevice(urlId);
        Map<String, Long> clicksByDevice = deviceTuples.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.getKey() != null ? tuple.getKey() : "unknown",
                        ClickCountTuple::getCount
                ));

        // 3. Tarik data Browser Tuple dan ubah menjadi Map<String, Long>
        List<ClickCountTuple> browserTuples = clickAnalyticsRepository.countClicksByBrowser(urlId);
        Map<String, Long> clicksByBrowser = browserTuples.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.getKey() != null ? tuple.getKey() : "unknown",
                        ClickCountTuple::getCount
                ));

        // 4. Bungkus ke dalam DTO sesuai kontrak dokumen API
        return ShortUrlAnalyticsResponse.builder()
                .success(true)
                .data(ShortUrlAnalyticsResponse.AnalyticsData.builder()
                        .shortCode(shortCode)
                        .totalClicks(totalClicks)
                        .clicksByDevice(clicksByDevice)
                        .clicksByBrowser(clicksByBrowser)
                        .build())
                .build();
    }

    /**
     * Logika untuk memperpendek URL dari Guest (Tanpa Login)
     */
    @Transactional
    public String createGuestUrl(ShortenRequest request) {
        // 1. Buat object Entity Url baru
        Url url = new Url();
        url.setLongUrl(request.getLongUrl());
        url.setExpiredAt(LocalDateTime.now().plusWeeks(1));
        url.setUser(null); // NULL karena dibuat oleh Guest

        // 2. Simpan dulu ke database PostgreSQL untuk memicu dan mendapatkan ID Auto-Increment (BIGSERIAL)
        Url savedUrl = urlRepository.save(url);

        // 3. Ambil ID tersebut, lalu masukkan ke Base62Service dengan trik offset 10 juta idemumu
        String shortCode = base62Service.encode(savedUrl.getId());

        // 4. Update data Url tadi dengan memasukkan hasil shortCode-nya
        savedUrl.setShortCode(shortCode);
        urlRepository.save(savedUrl);

        // 5. Kembalikan kode pendeknya ke Controller
        return shortCode;
    }

    @Transactional
    public ShortenUrlResponse createShortUrl(ShortenUrlRequest request, String currentUsername) {
        String finalShortCode;
        User currentUser = null;

        // 1. AMBIL DATA USER JIKA DIA LOG IN
        if (currentUsername != null && !currentUsername.equals("anonymousUser")) {
            currentUser = userRepository.findByUsername(currentUsername).orElse(null);
        }
        LocalDateTime requestedExpiry = request.getExpiredAt();

        if (requestedExpiry != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime maxAllowedExpiry = now.plusDays(30); // Batas maksimal 1 bulan

            // Proteksi 1: Jika tanggal yang diminta sudah lewat dari waktu sekarang (masa lalu)
            if (requestedExpiry.isBefore(now)) {
                throw new IllegalArgumentException("Tanggal kedaluwarsa tidak boleh di masa lalu!");
            }

            // Proteksi 2: Jika tanggal yang diminta melebihi 1 bulan dari sekarang
            if (requestedExpiry.isAfter(maxAllowedExpiry)) {
                throw new IllegalArgumentException("Waduh, batas maksimal masa aktif link adalah 30 hari!");
            }
        } else {
            // Kebijakan Bisnis: Jika user tidak mengisi expiredAt, kita set default 1 bulan dari sekarang
            // (Atau bisa kamu set null jika ingin user terdaftar berlaku selamanya)
            requestedExpiry = LocalDateTime.now().plusDays(30);
        }

        // 2. VALIDASI STRATEGI CUSTOM ALIAS
        if (request.getCustomAlias() != null && !request.getCustomAlias().trim().isEmpty()) {

            // Proteksi A: Jika GUEST mencoba mengirim custom alias -> Lempar IllegalAccessException (Nanti ditangkap sebagai 401)
            if (currentUser == null) {
                throw new SecurityException("Hanya user terdaftar yang boleh menggunakan fitur custom alias!");
            }

            String alias = request.getCustomAlias().trim().toLowerCase();

            // Proteksi B: Validasi karakter Alphanumeric & Dash (Hanya boleh huruf, angka, dan tanda minus)
            if (!alias.matches("^[a-zA-Z0-9-]+$")) {
                throw new IllegalArgumentException("Custom alias hanya boleh mengandung huruf, angka, dan tanda minus (-)!");
            }

            // Proteksi C: Cek apakah alias sudah dipakai
            if (urlRepository.existsByShortCode(alias)) {
                throw new IllegalArgumentException("Custom alias '" + alias + "' sudah terpakai!");
            }

            finalShortCode = alias;
        } else {
            // Jika tidak mengisi custom alias, gunakan sistem acak Base62 bawaan lama kita
            finalShortCode = null; // Biarkan null dulu untuk di-generate via Base62 setelah dapat ID database
        }

        // 3. PROSES SIMPAN KE DATABASE
        Url url = new Url();
        url.setLongUrl(request.getLongUrl());
        url.setShortCode(finalShortCode); // Bisa berupa teks alias atau null
        url.setExpiredAt(requestedExpiry);
        url.setUser(currentUser); // Hubungkan dengan user (akan bernilai null jika guest)

        url = urlRepository.save(url);

        // 4. GENERATE BASE62 JIKA BUKAN CUSTOM ALIAS
        if (finalShortCode == null) {
            // Taktik lama: panggil service base62 kamu menggunakan ID yang baru didapat
            // Anggap fungsi base62 service kamu bernama: base62Service.encode(url.getId())
            long offsetId = url.getId() + 10000000L;
            finalShortCode = base62Service.encode(offsetId); // Sesuaikan dengan util milikmu
            url.setShortCode(finalShortCode);
            urlRepository.save(url);
        }

        // 5. RACIK DTO RESPONSE SESUAI KONTRAK
        return ShortenUrlResponse.builder()
                .success(true)
                .message("Short URL created successfully")
                .data(ShortenUrlResponse.DataDetails.builder()
                        .shortCode(finalShortCode)
                        .longUrl(url.getLongUrl())
                        .shortUrl("http://localhost:8080/" + finalShortCode) // Gunakan domain idemu
                        .createdAt(url.getCreatedAt())
                        .expiredAt(requestedExpiry)
                        .createdBy(currentUser != null ? currentUser.getUsername() : "GUEST")
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public MyUrlsResponse getUserUrls(String currentUsername, int page, int size) {
        // 1. Proteksi: Jika guest mencoba mengakses, langsung tolak
        if (currentUsername == null || currentUsername.equals("anonymousUser")) {
            throw new SecurityException("Akses ditolak! Anda harus login untuk melihat daftar URL.");
        }

        // 2. Siapkan parameter Pagination Spring Data
        Pageable pageable = PageRequest.of(page, size);

        // 3. Tarik data terpaginasi dari Repository
        Page<MyUrlsResponse.UrlItem> urlPage = urlRepository.findUserUrlsWithClickCount(currentUsername, pageable);

        // 4. Bungkus ke dalam format DTO sesuai kontrak API kamu
        return MyUrlsResponse.builder()
                .success(true)
                .data(urlPage.getContent())
                .pageInfo(MyUrlsResponse.PageInfo.builder()
                        .currentPage(urlPage.getNumber())
                        .totalItems(urlPage.getTotalElements())
                        .totalPages(urlPage.getTotalPages())
                        .build())
                .build();
    }

    @Transactional
    public void deleteUrl(String shortCode, String currentUsername) {
        // 1. Proteksi: Jika guest mencoba menghapus, langsung tolak di tempat
        if (currentUsername == null || currentUsername.equals("anonymousUser")) {
            throw new SecurityException("Akses ditolak! Anda harus login untuk menghapus link.");
        }

        // 2. Ambil data URL dari PostgreSQL
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new IllegalArgumentException("Link pendek tidak ditemukan!"));

        // 3. Keamanan: Pastikan user yang login adalah BENAR pemilik link ini
        if (url.getUser() == null || !url.getUser().getUsername().equals(currentUsername)) {
            throw new AccessDeniedException("Akses ditolak! Anda tidak memiliki hak untuk menghapus link ini.");
        }

        // 4. STRATEGI OPSI B: Putuskan hubungan manual dengan ClickAnalytics (Set Null)
        if (url.getAnalytics() != null && !url.getAnalytics().isEmpty()) {
            for (ClickAnalytics click : url.getAnalytics()) {
                click.setUrl(null); // Memutus foreign key di DB menjadi NULL
            }
        }

        // 5. Eksekusi Hard Delete di PostgreSQL
        urlRepository.delete(url);

        // 6. CACHE EVICTION: Detik ini juga, hapus cache-nya di Redis agar link langsung mati total!
        String cacheKey = "urls::" + shortCode;
        redisTemplate.delete(cacheKey);

        System.out.println("====== [DELETE & EVICT SUCCESS] URL " + shortCode + " berhasil dimusnahkan! ======");
    }
}