package com.example.linkshortener.controller;

import com.example.linkshortener.dto.ShortenRequest;
import com.example.linkshortener.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/urls")
@CrossOrigin(origins = "*") // Mengizinkan React/aplikasi luar mengakses API ini (CORS)
public class UrlController {

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    /**
     * Endpoint untuk Guest membuat link pendek
     * POST http://localhost:8080/api/v1/urls/guest
     */
    @PostMapping("/guest")
    public ResponseEntity<Map<String, String>> createGuestShortLink(@Valid @RequestBody ShortenRequest request) {
        // Jalankan service untuk mendapatkan short code
        String shortCode = urlService.createGuestUrl(request);

        // Buat format URL lengkapnya
        String shortUrl = "http://localhost:8080/" + shortCode;

        // Kembalikan response JSON yang rapi
        Map<String, String> response = Map.of(
                "shortCode", shortCode,
                "shortUrl", shortUrl,
                "longUrl", request.getLongUrl()
        );

        return ResponseEntity.ok(response);
    }
}