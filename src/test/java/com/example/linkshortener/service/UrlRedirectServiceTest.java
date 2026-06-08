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
}