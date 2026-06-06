package com.example.linkshortener.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "users") // Memetakan kelas ini ke tabel bernama "users" di Postgres
@Data // Fitur Lombok: Otomatis membuat Getter, Setter, toString, dll.
@NoArgsConstructor // Fitur Lombok: Membuat constructor kosong wajib untuk JPA
@AllArgsConstructor // Fitur Lombok: Membuat constructor dengan semua parameter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Di Spring Boot 4.x/JPA modern, IDENTITY otomatis diterjemahkan menjadi BIGSERIAL di PostgreSQL
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private String updatedAt; // Kita gunakan String sementara waktu atau LocalDateTime sesuai kebutuhanmu

    // @PrePersist otomatis berjalan saat data pertama kali disimpan (INSERT)
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now().toString();
    }

    // @PreUpdate otomatis berjalan setiap kali data di-update di database
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now().toString();
    }
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private java.util.List<Url> urls = new java.util.ArrayList<>();
}
