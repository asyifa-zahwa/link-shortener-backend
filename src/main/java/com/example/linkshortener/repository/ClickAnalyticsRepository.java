package com.example.linkshortener.repository;

import com.example.linkshortener.dto.ClickCountTuple;
import com.example.linkshortener.entity.ClickAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClickAnalyticsRepository extends JpaRepository<ClickAnalytics, Long> {
    // Fungsi bawaan seperti save() otomatis bisa digunakan untuk mencatat data klik
    /**
     * Hitung total klik berdasarkan tipe perangkat untuk id URL tertentu
     */
    @Query(value = "SELECT device_type AS key, COUNT(*) AS count FROM click_analytics WHERE url_id = :urlId GROUP BY device_type", nativeQuery = true)
    List<ClickCountTuple> countClicksByDevice(@Param("urlId") Long urlId);

    /**
     * Hitung total klik berdasarkan rumpun browser untuk id URL tertentu
     */
    @Query(value = "SELECT browser_family AS key, COUNT(*) AS count FROM click_analytics WHERE url_id = :urlId GROUP BY browser_family", nativeQuery = true)
    List<ClickCountTuple> countClicksByBrowser(@Param("urlId") Long urlId);

    /**
     * Menghitung total klik keseluruhan dari suatu URL
     */
    Long countByUrlId(Long urlId);
}