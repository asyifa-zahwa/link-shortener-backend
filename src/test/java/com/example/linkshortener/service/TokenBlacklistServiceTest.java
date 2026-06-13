package com.example.linkshortener.service;

import com.example.linkshortener.config.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    private final String mockToken = "eyJhbGciOiJIUzI1NiJ9.mockTokenString.signature";
    private final String blacklistKey = "jwt:blacklist:" + mockToken;

    @BeforeEach
    void setUp() {
        // Hubungkan penyamaran opsForValue() agar mengembalikan objek valueOperations tiruan
        Mockito.lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Harus Sukses Memasukkan Token ke Blacklist Redis dengan TTL Sisa Umur Token")
    void blacklistToken_Success() {
        // GIVEN: Simulasikan token akan expired 10 menit (600.000 ms) dari sekarang
        long now = System.currentTimeMillis();
        Date futureExpiration = new Date(now + 600_000); // 10 menit ke depan

        Mockito.when(jwtUtils.getExpirationDateFromToken(mockToken)).thenReturn(futureExpiration);

        // WHEN: Eksekusi fungsi blacklist
        tokenBlacklistService.blacklistToken(mockToken);

        // THEN: Pastikan Redis menembak fungsi SET dengan key, value, dan TTL yang benar
        Mockito.verify(valueOperations, Mockito.times(1)).set(
                eq(blacklistKey),
                eq("blacklisted"),
                longThat(ttl -> ttl > 0 && ttl <= 600_000), // Memastikan TTL-nya bernilai positif dan sesuai sisa waktu
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    @DisplayName("Jangan Memasukkan ke Redis Jika Token Ternyata Sudah Expired")
    void blacklistToken_AlreadyExpired_ShouldNotSetToRedis() {
        // GIVEN: Simulasikan token ternyata sudah expired 5 menit lalu
        long now = System.currentTimeMillis();
        Date pastExpiration = new Date(now - 300_000); // 5 menit yang lalu

        Mockito.when(jwtUtils.getExpirationDateFromToken(mockToken)).thenReturn(pastExpiration);

        // WHEN
        tokenBlacklistService.blacklistToken(mockToken);

        // THEN: Pastikan Redis NEVER (tidak pernah) menyimpan token ini karena mubazir
        Mockito.verify(valueOperations, Mockito.never()).set(anyString(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("Harus Mengembalikan TRUE Jika Token Terdaftar di Blacklist Redis")
    void isTokenBlacklisted_True() {
        // GIVEN: Simulasikan Redis mendeteksi kunci blacklist tersebut ada
        Mockito.when(redisTemplate.hasKey(blacklistKey)).thenReturn(true);

        // WHEN
        boolean isBlacklisted = tokenBlacklistService.isTokenBlacklisted(mockToken);

        // THEN
        assertTrue(isBlacklisted, "Harusnya mengembalikan TRUE jika token ada di daftar hitam");
        Mockito.verify(redisTemplate, Mockito.times(1)).hasKey(blacklistKey);
    }

    @Test
    @DisplayName("Harus Mengembalikan FALSE Jika Token TIDAK Terdaftar di Blacklist Redis")
    void isTokenBlacklisted_False() {
        // GIVEN: Simulasikan Redis mendeteksi kunci blacklist tidak ada
        Mockito.when(redisTemplate.hasKey(blacklistKey)).thenReturn(false);

        // WHEN
        boolean isBlacklisted = tokenBlacklistService.isTokenBlacklisted(mockToken);

        // THEN
        assertFalse(isBlacklisted, "Harusnya mengembalikan FALSE jika token bersih dari blacklist");
        Mockito.verify(redisTemplate, Mockito.times(1)).hasKey(blacklistKey);
    }
}