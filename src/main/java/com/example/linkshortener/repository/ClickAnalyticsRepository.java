package com.example.linkshortener.repository;

import com.example.linkshortener.entity.ClickAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClickAnalyticsRepository extends JpaRepository<ClickAnalytics, Long> {
    // Fungsi bawaan seperti save() otomatis bisa digunakan untuk mencatat data klik
}