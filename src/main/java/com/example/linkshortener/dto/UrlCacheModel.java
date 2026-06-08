package com.example.linkshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UrlCacheModel implements Serializable {
    private Long id;
    private String shortCode;
    private String longUrl;
    private LocalDateTime expiredAt;
    private Long userId; // Jika null = milik Guest, jika ada angka = milik User Terdaftar
}