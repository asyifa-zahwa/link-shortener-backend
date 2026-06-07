package com.example.linkshortener.controller;

import com.example.linkshortener.dto.LoginRequest;
import com.example.linkshortener.dto.LoginResponse;
import com.example.linkshortener.dto.RegisterRequest;
import com.example.linkshortener.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        try {
            authService.registerUser(request);

            // Format response sesuai dengan ekspektasi kamu
            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "User registered successfully"
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            // Tangkap jika ada bad request (username/email duplikat)
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse loginResponse = authService.loginUser(request);

            // Return langsung objek DTO agar formatnya persis sesuai ekspektasimu
            return ResponseEntity.ok(Map.of(
                    "success", loginResponse.isSuccess(),
                    "tokenType", loginResponse.getTokenType(),
                    "accessToken", loginResponse.getAccessToken(),
                    "expiresIn", loginResponse.getExpiresIn()
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        // Bersihkan autentikasi dari thread lokal Spring Security saat ini
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Logged out successfully"
        ));
    }
}