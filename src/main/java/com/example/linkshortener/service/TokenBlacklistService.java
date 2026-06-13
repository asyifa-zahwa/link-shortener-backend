package com.example.linkshortener.service;

import com.example.linkshortener.config.JwtUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class TokenBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtUtils jwtUtils;

    public TokenBlacklistService(RedisTemplate<String, Object> redisTemplate, JwtUtils jwtUtils) {
        this.redisTemplate = redisTemplate;
        this.jwtUtils = jwtUtils;
    }

    /**
     * Memasukkan token ke dalam blacklist Redis dengan TTL sisa umur token tersebut
     */
    public void blacklistToken(String token) {
        try {
            Date expirationDate = jwtUtils.getExpirationDateFromToken(token);
            long now = System.currentTimeMillis();
            long remainingTimeInMs = expirationDate.getTime() - now;

            if (remainingTimeInMs > 0) {
                String blacklistKey = "jwt:blacklist:" + token;
                redisTemplate.opsForValue().set(
                        blacklistKey,
                        "blacklisted",
                        remainingTimeInMs,
                        TimeUnit.MILLISECONDS
                );
            }
        } catch (Exception e) {
            // Jika terjadi error (misal token sudah expired duluan saat diparsing), biarkan saja
        }
    }

    /**
     * Memeriksa apakah token terdaftar di dalam blacklist Redis
     */
    public boolean isTokenBlacklisted(String token) {
        String blacklistKey = "jwt:blacklist:" + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
    }
}