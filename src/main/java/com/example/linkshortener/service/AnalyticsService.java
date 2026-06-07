package com.example.linkshortener.service;

import com.example.linkshortener.entity.ClickAnalytics;
import com.example.linkshortener.entity.Url;
import com.example.linkshortener.repository.ClickAnalyticsRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AnalyticsService {

    private final ClickAnalyticsRepository clickAnalyticsRepository;

    public AnalyticsService(ClickAnalyticsRepository clickAnalyticsRepository) {
        this.clickAnalyticsRepository = clickAnalyticsRepository;
    }

    /**
     * Fungsi ini akan dieksekusi di thread terpisah (Latar Belakang)
     */
    @Async // <-- Sihir utamanya ada di sini!
    @Transactional
    public void recordClickAnalyticsAsync(Url url, String ipAddress, String userAgent, String referrer) {
        // Simulasikan log di konsol untuk bukti kalau ini berjalan asinkron
        System.out.println("Async Thread [" + Thread.currentThread().getName() + "] mulai mencatat analitik...");

        ClickAnalytics analytics = new ClickAnalytics();
        analytics.setUrl(url);
        analytics.setIpAddress(ipAddress);
        analytics.setUserAgent(userAgent);
        analytics.setReferrer(referrer != null ? referrer : "Direct Click");

        analytics.setBrowserFamily("Mendeteksi...");
        analytics.setDeviceType("Mendeteksi...");
        analytics.setOperatingSystem("Mendeteksi...");

        clickAnalyticsRepository.save(analytics);

        System.out.println("Async Thread [" + Thread.currentThread().getName() + "] SelesAI menyimpan ke database!");
    }
}