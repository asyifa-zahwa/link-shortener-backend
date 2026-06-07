package com.example.linkshortener.controller;

import com.example.linkshortener.service.UrlRedirectService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class RedirectController {

    private final UrlRedirectService redirectService;

    public RedirectController(UrlRedirectService redirectService) {
        this.redirectService = redirectService;
    }

    /**
     * Menangani HTTP Redirect 302 ketika short link diakses
     * GET http://localhost:8080/{shortCode}
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> handleRedirect(@PathVariable String shortCode, HttpServletRequest request) {
        try {
            // Ambil URL asli dari service
            String originalUrl = redirectService.getOriginalUrlAndRecordClick(shortCode, request);

            // Gunakan HTTP Status 302 Found (Standard Redirect Industri)
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(originalUrl))
                    .build();

        } catch (IllegalArgumentException | IllegalStateException e) {
            // Jika link tidak ada atau expired, lempar ke halaman error atau beri respon 404
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}