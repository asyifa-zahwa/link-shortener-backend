package com.example.linkshortener.repository;

import com.example.linkshortener.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Query otomatis dibentuk oleh Spring Data JPA untuk mencari berdasarkan username
    Optional<User> findByUsername(String username);

    // Query otomatis untuk mengecek apakah email sudah terdaftar
    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}