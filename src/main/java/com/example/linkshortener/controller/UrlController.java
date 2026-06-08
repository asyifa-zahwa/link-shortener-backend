package com.example.linkshortener.controller;

import com.example.linkshortener.dto.ShortenRequest;
import com.example.linkshortener.dto.ShortenUrlRequest;
import com.example.linkshortener.dto.ShortenUrlResponse;
import com.example.linkshortener.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    @PostMapping
    public ResponseEntity<Object> createShortenUrl(
            @Valid @RequestBody ShortenUrlRequest request) { // Spring Security otomatis menyuntikkan data token ke sini

        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            // Jika token tidak ada, authentication akan bernilai null
            String currentUsername = (authentication != null) ? authentication.getName() : null;

            ShortenUrlResponse response = urlService.createShortUrl(request, currentUsername);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (SecurityException e) {
            // 413 / 401 Unauthorized khusus Guest yang nekat pakai alias
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        } catch (IllegalArgumentException e) {
            // 400 Bad Request jika alias duplikat atau mengandung karakter terlarang
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}