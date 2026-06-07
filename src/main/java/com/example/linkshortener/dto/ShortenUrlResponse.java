package com.example.linkshortener.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ShortenUrlResponse {
    private boolean success;
    private String message;
    private DataDetails data;

    @Data
    @Builder
    public static class DataDetails {
        private String shortCode;
        private String longUrl;
        private String shortUrl;
        private LocalDateTime createdAt;
        private LocalDateTime expiredAt;
        private String createdBy; // Berisi username atau "GUEST"
    }
}