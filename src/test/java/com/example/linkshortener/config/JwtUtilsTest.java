package com.example.linkshortener.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private final String testUsername = "asyifa@developer.com";

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
    }

    @Test
    @DisplayName("Harus sukses membuat token JWT yang valid")
    void generateToken_Success() {
        String token = jwtUtils.generateToken(testUsername);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        // Token JWT standar selalu terdiri dari 3 bagian yang dipisahkan oleh tanda titik (header.payload.signature)
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    @DisplayName("Harus sukses mengambil kembali username dari dalam token")
    void getUsernameFromToken_Success() {
        String token = jwtUtils.generateToken(testUsername);

        String extractedUsername = jwtUtils.getUsernameFromToken(token);

        assertEquals(testUsername, extractedUsername);
    }

    @Test
    @DisplayName("Harus mengembalikan true untuk token yang valid")
    void validateToken_Success() {
        String token = jwtUtils.generateToken(testUsername);

        boolean isValid = jwtUtils.validateToken(token);

        assertTrue(isValid);
    }

    @Test
    @DisplayName("Harus mengembalikan false jika token palsu atau rusak")
    void validateToken_InvalidToken_ReturnsFalse() {
        String tokenPalsu = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.palsu.palsu";

        boolean isValid = jwtUtils.validateToken(tokenPalsu);

        assertFalse(isValid);
    }
}