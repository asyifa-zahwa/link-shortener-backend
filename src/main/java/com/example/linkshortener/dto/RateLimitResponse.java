package com.example.linkshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class RateLimitResponse {
    private boolean success;
    private String message;
    private long limitMax;
    private long remaining;
    private LocalDateTime timestamp;
}