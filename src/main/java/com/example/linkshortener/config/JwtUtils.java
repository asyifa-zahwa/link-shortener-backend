package com.example.linkshortener.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {

    // Kunci rahasia minimal harus 32 karakter (256-bit) untuk algoritma HS256
    private static final String SECRET_STRING = "kamu-wajib-mengubah-ini-menjadi-sangat-rahasia-dan-panjang-minimal-32-karakter";
    private final SecretKey key = Keys.hmacShaKeyFor(SECRET_STRING.getBytes(StandardCharsets.UTF_8));

    // Token kedaluwarsa dalam 1 hari (dalam milidetik)
    private static final long EXPIRATION_TIME = 86400000L;

    /**
     * Membuat Token JWT baru berdasarkan username/email user
     */
    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key)
                .compact();
    }

    /**
     * Mengambil username/email dari dalam Token JWT
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    /**
     * Validasi apakah token JWT asli dan belum kedaluwarsa
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Token palsu, rusak, atau sudah expired akan ditangkap di sini
            return false;
        }
    }
}