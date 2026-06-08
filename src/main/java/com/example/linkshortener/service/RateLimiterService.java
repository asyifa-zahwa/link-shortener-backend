package com.example.linkshortener.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RateLimiterService {

    private final RedisTemplate<String, Object> redisTemplate;

    public RateLimiterService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Memeriksa apakah request masih diperbolehkan atau sudah terkena limit.
     * @param key Identifikator unik di Redis (contoh: rate:click:FX9N::192.168.1.1)
     * @param maxLimit Batas maksimum request yang diizinkan
     * @param ttlTime Durasi waktu window
     * @param timeUnit Satuan waktu (Detik untuk klik, Jam/Hari untuk pembuatan link)
     * @return true jika lolos (aman), false jika terkena rate limit
     */
    public boolean isAllowed(String key, long maxLimit, long ttlTime, TimeUnit timeUnit) {
        // Menggunakan perintah INCR atomik milik Redis
        Long currentCount = redisTemplate.opsForValue().increment(key);

        if (currentCount == null) {
            return true;
        }

        // Jika ini adalah request pertama di window ini, pasang waktu expired
        if (currentCount == 1) {
            redisTemplate.expire(key, ttlTime, timeUnit);
        }

        // Jika hitungan melewati batas, potong akses!
        return currentCount <= maxLimit;
    }

    /**
     * Mendapatkan sisa kuota request yang tersedia untuk diinfokan ke user
     */
    public long getRemainingQuota(String key, long maxLimit) {
        Integer currentCount = (Integer) redisTemplate.opsForValue().get(key);
        if (currentCount == null) {
            return maxLimit;
        }
        long remaining = maxLimit - currentCount;
        return Math.max(0, remaining);
    }
}