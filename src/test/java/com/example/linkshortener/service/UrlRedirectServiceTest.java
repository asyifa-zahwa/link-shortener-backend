package com.example.linkshortener.service;

import com.example.linkshortener.dto.UrlCacheModel;
import com.example.linkshortener.entity.Url;
import com.example.linkshortener.repository.UrlRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class UrlRedirectServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private UrlRedirectService urlRedirectService;

    private final String shortCode = "FX9N";
    private final String cacheKey = "urls::" + shortCode;
    private Url dummyUrl;

    @BeforeEach
    void setUp() {
        Mockito.lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        dummyUrl = new Url();
        dummyUrl.setId(12L);
        dummyUrl.setShortCode(shortCode);
        dummyUrl.setLongUrl("https://google.com");
        dummyUrl.setExpiredAt(LocalDateTime.now().plusDays(7));
    }

    @Test
    @DisplayName("Harus Sukses Lewat Jalur CACHE HIT (Milik User Terdaftar - Picu Analitik Tanpa Sentuh DB)")
    void getOriginalUrlAndRecordClick_CacheHit_RegisteredUser() {
        // GIVEN: userId terisi (100L), menandakan milik user terdaftar
        UrlCacheModel mockCache = new UrlCacheModel(12L, shortCode, "https://google.com", LocalDateTime.now().plusDays(1), 100L);
        Mockito.when(valueOperations.get(cacheKey)).thenReturn(mockCache);
        Mockito.when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");

        // WHEN
        String originalUrl = urlRedirectService.getOriginalUrlAndRecordClick(shortCode, request);

        // THEN
        assertEquals("https://google.com", originalUrl);
        Mockito.verify(valueOperations, Mockito.times(1)).get(cacheKey);

        // PENTING: PostgreSQL 100% tidak tersentuh untuk query SELECT pencarian URL
        Mockito.verify(urlRepository, Mockito.never()).findByShortCode(anyString());

        // Analitik HARUS tetap dipicu karena ini link milik user terdaftar
        Mockito.verify(analyticsService, Mockito.times(1)).recordClickAnalyticsAsync(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Harus Sukses Lewat Jalur CACHE HIT (Milik Guest - Lewatkan Pengalihan Secepat Kilat TANPA ANALITIK)")
    void getOriginalUrlAndRecordClick_CacheHit_GuestUser() {
        // GIVEN: userId bernilai null, menandakan link buatan Guest
        UrlCacheModel mockCache = new UrlCacheModel(12L, shortCode, "https://google.com", LocalDateTime.now().plusDays(1), null);
        Mockito.when(valueOperations.get(cacheKey)).thenReturn(mockCache);

        // WHEN
        String originalUrl = urlRedirectService.getOriginalUrlAndRecordClick(shortCode, request);

        // THEN
        assertEquals("https://google.com", originalUrl);

        // PENTING: Jika link milik Guest, analitik TIDAK BOLEH dipanggil sama sekali demi hemat RAM & DB
        Mockito.verify(analyticsService, Mockito.never()).recordClickAnalyticsAsync(any(), any(), any(), any());
        Mockito.verify(urlRepository, Mockito.never()).findByShortCode(anyString());
    }

    @Test
    @DisplayName("Harus Sukses Lewat Jalur CACHE MISS (Ambil dari Postgres, Simpan ke Redis dengan Jitter, Jalankan Analitik)")
    void getOriginalUrlAndRecordClick_CacheMiss_Success() {
        // GIVEN: Redis kosong melompong
        Mockito.when(valueOperations.get(cacheKey)).thenReturn(null);
        Mockito.when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(dummyUrl));
//        Mockito.when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");

        // WHEN
        String originalUrl = urlRedirectService.getOriginalUrlAndRecordClick(shortCode, request);

        // THEN
        assertEquals("https://google.com", originalUrl);

        // Memastikan data disimpan ke Redis secara otomatis (Lazy Loading)
        Mockito.verify(valueOperations, Mockito.times(1)).set(
                eq(cacheKey),
                any(UrlCacheModel.class),
                anyLong(),
                eq(TimeUnit.MINUTES)
        );
    }
    @Test
    @DisplayName("Jalur CACHE HIT: Harus Melempar Error dan Menghapus Cache Jika Mendeteksi Link Kedaluwarsa di Memori")
    void getOriginalUrlAndRecordClick_CacheHit_ButExpired_ShouldDeleteCacheAndThrowError() {
        // GIVEN: Simulasikan data ada di Redis (Cache Hit), tapi expiredAt bernilai 5 menit yang lalu
        LocalDateTime pastExpiry = LocalDateTime.now().minusMinutes(5);
        UrlCacheModel expiredCache = new UrlCacheModel(12L, shortCode, "https://google.com", pastExpiry, 100L);

        Mockito.when(valueOperations.get(cacheKey)).thenReturn(expiredCache);

        // WHEN & THEN: Pastikan aplikasi melempar IllegalStateException
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            urlRedirectService.getOriginalUrlAndRecordClick(shortCode, request);
        });

        assertEquals("Link pendek ini sudah kedaluwarsa!", exception.getMessage());

        // PENTING: Pastikan jaring pengaman darurat kita menghapus cache basi tersebut dari Redis
        Mockito.verify(redisTemplate, Mockito.times(1)).delete(cacheKey);

        // PostgreSQL dan Analitik tidak boleh disentuh karena proses dihentikan seketika
        Mockito.verify(urlRepository, Mockito.never()).findByShortCode(anyString());
        Mockito.verify(analyticsService, Mockito.never()).recordClickAnalyticsAsync(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Jalur CACHE MISS: Harus Melempar Error Jika Data di Database Ternyata Sudah Kedaluwarsa")
    void getOriginalUrlAndRecordClick_CacheMiss_ButDatabaseExpired_ShouldThrowError() {
        // GIVEN: Redis kosong, tapi data di database ternyata sudah expired 1 hari yang lalu
        dummyUrl.setExpiredAt(LocalDateTime.now().minusDays(1));

        Mockito.when(valueOperations.get(cacheKey)).thenReturn(null);
        Mockito.when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(dummyUrl));

        // WHEN & THEN: Harus melempar error saat mengecek data dari database
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            urlRedirectService.getOriginalUrlAndRecordClick(shortCode, request);
        });

        assertEquals("Link pendek ini sudah kedaluwarsa!", exception.getMessage());

        // PENTING: Karena sudah expired, data TIDAK BOLEH disimpan ke Redis
        Mockito.verify(valueOperations, Mockito.never()).set(anyString(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("Jalur CACHE MISS: Harus Menyimpan ke Redis dengan Dynamic TTL Jika Sisa Umur Link Sangat Singkat")
    void getOriginalUrlAndRecordClick_CacheMiss_WithDynamicTTL() {
        // GIVEN: Redis kosong, dan link di database akan expired dalam 30 menit ke depan (Skenario Guest/User mepet)
        LocalDateTime shortExpiry = LocalDateTime.now().plusMinutes(30);
        dummyUrl.setExpiredAt(shortExpiry);

        Mockito.when(valueOperations.get(cacheKey)).thenReturn(null);
        Mockito.when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(dummyUrl));

        // WHEN
        String originalUrl = urlRedirectService.getOriginalUrlAndRecordClick(shortCode, request);

        // THEN
        assertEquals("https://google.com", originalUrl);

        // KUNCI STRATEGI 2: Pastikan nilai TTL yang masuk ke Redis bernilai <= 30 menit (Math.min bekerja!)
        // Kita gunakan ArgumentCaptor atau custom matcher longThat untuk memverifikasi angka menitnya
        Mockito.verify(valueOperations, Mockito.times(1)).set(
                eq(cacheKey),
                any(UrlCacheModel.class),
                Mockito.longThat(ttl -> ttl > 0 && ttl <= 30), // Memastikan TTL dinamis bernilai maksimal 30 menit
                eq(TimeUnit.MINUTES)
        );
    }
}