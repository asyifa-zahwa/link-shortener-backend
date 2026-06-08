package com.example.linkshortener.service;

import com.example.linkshortener.dto.MyUrlsResponse;
import com.example.linkshortener.dto.ShortenRequest;
import com.example.linkshortener.dto.ShortenUrlRequest;
import com.example.linkshortener.dto.ShortenUrlResponse;
import com.example.linkshortener.entity.Url;
import com.example.linkshortener.entity.User;
import com.example.linkshortener.repository.UrlRepository;
import com.example.linkshortener.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UrlService {

    private final UrlRepository urlRepository;
    private final Base62Service base62Service;
    private final UserRepository userRepository;

    // Dependency Injection melalui Constructor
    public UrlService(UrlRepository urlRepository, Base62Service base62Service, UserRepository userRepository) {
        this.urlRepository = urlRepository;
        this.base62Service = base62Service;
        this.userRepository = userRepository;
    }

    /**
     * Logika untuk memperpendek URL dari Guest (Tanpa Login)
     */
    @Transactional
    public String createGuestUrl(ShortenRequest request) {
        // 1. Buat object Entity Url baru
        Url url = new Url();
        url.setLongUrl(request.getLongUrl());
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
        url.setExpiredAt(request.getExpiredAt());
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
                        .expiredAt(url.getExpiredAt())
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
}