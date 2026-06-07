package com.example.linkshortener.service;

import com.example.linkshortener.entity.Url;
import com.example.linkshortener.repository.UrlRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UrlRedirectService {

    private final UrlRepository urlRepository;
    private final AnalyticsService analyticsService; // <-- Inject Service baru

    public UrlRedirectService(UrlRepository urlRepository, AnalyticsService analyticsService) {
        this.urlRepository = urlRepository;
        this.analyticsService = analyticsService;
    }

    @Transactional(readOnly = true)
    public String getOriginalUrlAndRecordClick(String shortCode, HttpServletRequest request) {
        // 1. Cari URL asli
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new IllegalArgumentException("Link pendek tidak ditemukan!"));

        if (url.getExpiredAt() != null && url.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Link pendek ini sudah kedaluwarsa!");
        }

        // 2. Ekstrak metadata metadata browser secara mentah di thread utama
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        }
        String userAgent = request.getHeader("User-Agent");
        String referrer = request.getHeader("Referer");

        // 3. Tembak ke fungsi Async! (Java akan langsung melompat ke baris berikutnya tanpa menunggu fungsi ini kelar)
        analyticsService.recordClickAnalyticsAsync(url, ipAddress, userAgent, referrer);

        // 4. Langsung kembalikan URL asli demi kecepatan kilat redirection
        return url.getLongUrl();
    }
}