package com.example.linkshortener.service;

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

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RateLimiterService rateLimiterService;

    private final String mockKey = "rate:click:FX9N::192.168.1.1:15";

    @BeforeEach
    void setUp() {
        // Hubungkan penyamaran opsForValue() agar mengembalikan objek valueOperations tiruan
        Mockito.lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Harus Lolos dan Memasang TTL Jika Request adalah Klik Pertama (Count == 1)")
    void isAllowed_FirstRequest_ShouldSetTTL() {
        // GIVEN: Simulasi perintah INCR Redis mengembalikan angka 1 (klik pertama)
        Mockito.when(valueOperations.increment(mockKey)).thenReturn(1L);

        // WHEN: Panggil service dengan limit batas maks 3
        boolean allowed = rateLimiterService.isAllowed(mockKey, 3, 1, TimeUnit.SECONDS);

        // THEN
        assertTrue(allowed, "Request pertama harusnya lolos (true)");

        // KUNCI UTAMA: Pastikan perintah EXPIRE wajib ditembak 1x untuk mengunci durasi waktu!
        Mockito.verify(redisTemplate, Mockito.times(1)).expire(mockKey, 1, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("Harus Tetap Lolos Tapi TANPA Memasang TTL Jika Request adalah Klik Kedua (Count == 2)")
    void isAllowed_SecondRequest_ShouldNotSetTTLAgain() {
        // GIVEN: Perintah INCR mengembalikan angka 2 (klik kedua)
        Mockito.when(valueOperations.increment(mockKey)).thenReturn(2L);

        // WHEN
        boolean allowed = rateLimiterService.isAllowed(mockKey, 3, 1, TimeUnit.SECONDS);

        // THEN
        assertTrue(allowed, "Klik kedua (2 <= 3) harusnya tetap lolos");

        // KUNCI UTAMA: Jangan pasang EXPIRE lagi jika sudah klik kedua demi menghemat komputasi Redis
        Mockito.verify(redisTemplate, Mockito.never()).expire(anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("Harus Memblokir Akses (Return False) Jika Angka Hitungan Melewati Batas Maksimum")
    void isAllowed_OverLimit_ShouldBlock() {
        // GIVEN: Perintah INCR mengembalikan angka 4 (sudah melanggar batas maks 3)
        Mockito.when(valueOperations.increment(mockKey)).thenReturn(4L);

        // WHEN
        boolean allowed = rateLimiterService.isAllowed(mockKey, 3, 1, TimeUnit.SECONDS);

        // THEN
        assertFalse(allowed, "Hitungan ke-4 wajib diblokir (false) karena batas maks cuma 3");
        Mockito.verify(redisTemplate, Mockito.never()).expire(anyString(), anyLong(), any());
    }
}