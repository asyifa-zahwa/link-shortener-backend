package com.example.linkshortener.controller;

import com.example.linkshortener.dto.MyUrlsResponse;
import com.example.linkshortener.service.UrlService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/my-urls")
@CrossOrigin(origins = "*")
public class MyUrlsController {

    private final UrlService urlService;

    public MyUrlsController(UrlService urlService) {
        this.urlService = urlService;
    }

    @GetMapping
    public ResponseEntity<Object> getMyUrls(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            // Ambil username dari token JWT yang lolos dari filter satpam
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = (authentication != null) ? authentication.getName() : null;

            MyUrlsResponse response = urlService.getUserUrls(currentUsername, page, size);
            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            // Error Response jika mencoba mengakses tanpa JWT token valid (401 Unauthorized)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            // Penanganan error tak terduga lainnya (500 Internal Server Error)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Terjadi kesalahan internal: " + e.getMessage()
            ));
        }
    }
}