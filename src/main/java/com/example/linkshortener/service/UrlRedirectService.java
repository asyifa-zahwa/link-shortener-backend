package com.example.linkshortener.service;

import com.example.linkshortener.dto.UrlCacheModel;
import com.example.linkshortener.entity.Url;
import com.example.linkshortener.repository.UrlRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class UrlRedirectService {

    private final UrlRepository urlRepository;
    private final AnalyticsService analyticsService;
    private final RedisTemplate<String, Object> redisTemplate;

    public UrlRedirectService(UrlRepository urlRepository, AnalyticsService analyticsService, RedisTemplate<String, Object> redisTemplate) {
        this.urlRepository = urlRepository;
        this.analyticsService = analyticsService;
        this.redisTemplate = redisTemplate;
    }

    @Transactional(readOnly = true)
    public String getOriginalUrlAndRecordClick(String shortCode, HttpServletRequest request) {
        String cacheKey = "urls::" + shortCode;

        // 1. Coba ambil data dari RAM Redis
        UrlCacheModel cacheModel = (UrlCacheModel) redisTemplate.opsForValue().get(cacheKey);

        // KONDISI A: CACHE HIT (0% Query SELECT ke PostgreSQL untuk cari URL)
        if (cacheModel != null) {
            // Validasi apakah link sudah expired di level aplikasi
            if (cacheModel.getExpiredAt() != null && cacheModel.getExpiredAt().isBefore(LocalDateTime.now())) {
                redisTemplate.delete(cacheKey); // Bersihkan cache basi
                throw new IllegalStateException("Link pendek ini sudah kedaluwarsa!");
            }

            // FILTER IMPIAN: Hanya proses analitik jika link ini ada pemiliknya (Bukan milik Guest)
            if (cacheModel.getUserId() != null) {
                Url dummyUrl = new Url();
                dummyUrl.setId(cacheModel.getId());
                dummyUrl.setShortCode(cacheModel.getShortCode());
                dummyUrl.setLongUrl(cacheModel.getLongUrl());

                triggerAsyncAnalytics(dummyUrl, request);
            }

            return cacheModel.getLongUrl();
        }

        // KONDISI B: CACHE MISS (Hanya terjadi pada klik pertama setelah cache expired)
        synchronized (this) {
            // Double-checked locking untuk menghindari Cache Stampede
            cacheModel = (UrlCacheModel) redisTemplate.opsForValue().get(cacheKey);
            if (cacheModel != null) {
                return cacheModel.getLongUrl();
            }

            System.out.println("====== [CACHE MISS] Ketuk PostgreSQL untuk shortCode: " + shortCode + " ======");
            Url url = urlRepository.findByShortCode(shortCode)
                    .orElseThrow(() -> new IllegalArgumentException("Link pendek tidak ditemukan!"));

            if (url.getExpiredAt() != null && url.getExpiredAt().isBefore(LocalDateTime.now())) {
                throw new IllegalStateException("Link pendek ini sudah kedaluwarsa!");
            }

            // Ambil ID User jika ada (jika guest, url.getUser() akan null)
            Long ownerId = (url.getUser() != null) ? url.getUser().getId() : null;

            // Bungkus data ke dalam Model Cache
            UrlCacheModel newCache = new UrlCacheModel(
                    url.getId(),
                    url.getShortCode(),
                    url.getLongUrl(),
                    url.getExpiredAt(),
                    ownerId
            );

            // Setel TTL Jitter: 24 Jam (1440 Menit) + Acak hingga 3 jam (180 Menit)
            long randomMinutes = 1440 + new java.util.Random().nextInt(180);
            redisTemplate.opsForValue().set(cacheKey, newCache, randomMinutes, TimeUnit.MINUTES);

            // Jika bukan milik guest, pemicu analitik async dijalankan
            if (ownerId != null) {
                triggerAsyncAnalytics(url, request);
            }

            return url.getLongUrl();
        }
    }

    private void triggerAsyncAnalytics(Url url, HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        }
        String userAgent = request.getHeader("User-Agent");
        String referrer = request.getHeader("Referer");

        analyticsService.recordClickAnalyticsAsync(url, ipAddress, userAgent, referrer);
    }
}