package com.example.linkshortener.repository;

import com.example.linkshortener.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {

    // Fungsi ini yang akan dipanggil setiap kali ada orang klik link pendek (Redirection)
    Optional<Url> findByShortCode(String shortCode);

    // Fungsi untuk mengecek apakah custom alias sudah dipakai orang lain
    boolean existsByShortCode(String shortCode);
}