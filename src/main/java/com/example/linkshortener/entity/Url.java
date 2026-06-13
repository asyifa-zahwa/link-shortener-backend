package com.example.linkshortener.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "urls") // Memetakan kelas ini ke tabel "urls" di Postgres
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Url {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Otomatis dipetakan ke BIGSERIAL di Postgres
    private Long id;

    @Column(name = "short_code", unique = true, nullable = true, length = 30)
    // unique = true otomatis membuat Unique B-Tree Index untuk menangani race condition & lookup cepat
    private String shortCode;

    @Column(name = "long_url", nullable = false, columnDefinition = "TEXT")
    // Menggunakan TEXT karena URL asli bisa sangat panjang (lebih dari 255 karakter)
    private String longUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    // Relasi Many-to-One: Banyak URL bisa dimiliki oleh satu User. LAZY digunakan agar performa lebih cepat.
    @JoinColumn(name = "user_id", nullable = true)
    // nullable = true mendukung fitur Guest (URL dibuat tanpa harus login)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expired_at") // Boleh null, artinya link berlaku selamanya
    private LocalDateTime expiredAt;

    // Otomatis mengisi waktu dibuat sebelum data di-INSERT ke database
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
    @OneToMany(mappedBy = "url", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private java.util.List<ClickAnalytics> analytics = new java.util.ArrayList<>();
}