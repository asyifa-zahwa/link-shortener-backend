package com.example.linkshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private boolean success;
    private String tokenType;
    private String accessToken;
    private long expiresIn; // Nilai dalam detik (86400)
}