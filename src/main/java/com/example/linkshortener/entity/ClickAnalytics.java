package com.example.linkshortener.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "click_analytics") // Memetakan kelas ini ke tabel "click_analytics" di Postgres
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClickAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Dipetakan ke BIGSERIAL di Postgres
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "url_id", nullable = false)
    // nullable = false karena tidak mungkin ada data klik tanpa ada link pendeknya
    private Url url;

    @Column(name = "clicked_at", nullable = false, updatable = false)
    private LocalDateTime clickedAt;

    @Column(name = "referrer", length = 255)
    private String referrer;

    @Column(name = "device_type", length = 20)
    private String deviceType;

    @Column(name = "browser_family", length = 50)
    private String browserFamily;

    @Column(name = "operating_system", length = 50)
    private String operatingSystem;

    @Column(name = "ip_address", length = 45)
    // Panjang 45 karakter aman untuk menampung format IPv4 maupun IPv6
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    // Menggunakan TEXT karena string User-Agent bawaan browser biasanya sangat panjang dan detail
    private String userAgent;

    // Otomatis mencatat waktu klik tepat sebelum data di-INSERT ke database
    @PrePersist
    protected void onPersist() {
        this.clickedAt = LocalDateTime.now();
    }
}