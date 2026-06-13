package com.example.linkshortener.service;

import com.example.linkshortener.entity.ClickAnalytics;
import com.example.linkshortener.entity.Url;
import com.example.linkshortener.repository.ClickAnalyticsRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua_parser.Client;
import ua_parser.Parser;

import java.time.LocalDateTime;

@Service
public class AnalyticsService {

    private final ClickAnalyticsRepository clickAnalyticsRepository;
    private final Parser uaParser;
    public AnalyticsService(ClickAnalyticsRepository clickAnalyticsRepository, Parser uaParser) {
        this.uaParser = uaParser;
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

        if (userAgent != null && !userAgent.isEmpty()) {
            try {
                Client client = uaParser.parse(userAgent);

                // 1. Ekstrak Browser Family (e.g., Chrome, Safari, Firefox)
                analytics.setBrowserFamily(client.userAgent.family.toLowerCase());

                // 2. Ekstrak Operating System (e.g., Windows, Android, iOS)
                analytics.setOperatingSystem(client.os.family.toLowerCase());

                // 3. Tentukan Device Type berdasarkan karakteristik OS / User-Agent
                String os = client.os.family.toLowerCase();
                String ua = userAgent.toLowerCase();

                if (os.contains("android") || os.contains("ios") || ua.contains("mobile")) {
                    analytics.setDeviceType("mobile");
                } else if (ua.contains("tablet") || ua.contains("ipad")) {
                    analytics.setDeviceType("tablet");
                } else {
                    analytics.setDeviceType("desktop"); // Default untuk laptop/PC
                }

            } catch (Exception e) {
                // Jaring pengaman jika user-agent aneh/rusak gagal diparsing
                analytics.setBrowserFamily("unknown");
                analytics.setOperatingSystem("unknown");
                analytics.setDeviceType("desktop");
            }
        } else {
            analytics.setBrowserFamily("unknown");
            analytics.setOperatingSystem("unknown");
            analytics.setDeviceType("desktop");
            analytics.setReferrer("direct");
        }

        clickAnalyticsRepository.save(analytics);

        System.out.println("Async Thread [" + Thread.currentThread().getName() + "] SelesAI menyimpan ke database!");
    }
}