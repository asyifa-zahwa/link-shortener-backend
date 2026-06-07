package com.example.linkshortener.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ShortenUrlRequest {

    @NotBlank(message = "Long URL wajib diisi!")
    private String longUrl;

    // Bersifat opsional, hanya boleh diisi oleh user terdaftar
    private String customAlias;

    // Bersifat opsional, jika kosong berarti link berlaku selamanya
    private LocalDateTime expiredAt;
}