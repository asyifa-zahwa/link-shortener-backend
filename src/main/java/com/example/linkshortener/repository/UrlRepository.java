package com.example.linkshortener.repository;

import com.example.linkshortener.dto.MyUrlsResponse;
import com.example.linkshortener.entity.Url;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {

    // Fungsi ini yang akan dipanggil setiap kali ada orang klik link pendek (Redirection)
    Optional<Url> findByShortCode(String shortCode);

    // Fungsi untuk mengecek apakah custom alias sudah dipakai orang lain
    boolean existsByShortCode(String shortCode);
    /**
     * Query sakti untuk menarik data URL sekaligus menghitung total klik secara real-time per user
     */
    @Query("SELECT new com.example.linkshortener.dto.MyUrlsResponse$UrlItem(" +
            "u.shortCode, u.longUrl, CONCAT('http://localhost:8080/', u.shortCode), COUNT(c), u.createdAt) " +
            "FROM Url u " +
            "LEFT JOIN ClickAnalytics c ON c.url = u " +
            "WHERE u.user.username = :username " +
            "GROUP BY u.id, u.shortCode, u.longUrl, u.createdAt " +
            "ORDER BY u.createdAt DESC")
    Page<MyUrlsResponse.UrlItem> findUserUrlsWithClickCount(@Param("username") String username, Pageable pageable);

    /**
     * Menyapu bersih semua baris data yang expiredAt nya sudah melewati waktu parameter
     */
    void deleteByExpiredAtBefore(LocalDateTime dateTime);
}