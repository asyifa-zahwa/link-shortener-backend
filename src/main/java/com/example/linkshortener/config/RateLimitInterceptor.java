package com.example.linkshortener.config;

import com.example.linkshortener.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;

    // Sekarang constructor sangat bersih, hanya membutuhkan RateLimiterService
    public RateLimitInterceptor(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String ipAddress = getClientIp(request);

        // ==========================================
        // JALUR 1: PROTEKSI KLIK LINK (GET /{shortCode})
        // ==========================================
        if (method.equals("GET") && uri.length() > 1 && !uri.startsWith("/api/v1")) {
            String shortCode = uri.substring(1); // Ambil kode setelah tanda '/'
            String redisKey = "rate:click:" + shortCode + "::" + ipAddress + ":" + LocalDateTime.now().getSecond();

            long maxClickPerSecond = 3; // Batasan seragam: 3 klik per detik per IP

            if (!rateLimiterService.isAllowed(redisKey, maxClickPerSecond, 1, TimeUnit.SECONDS)) {
                sendManualErrorResponse(response, "Waduh, kamu mengeklik tautan terlalu cepat! Harap tunggu 1 detik.", maxClickPerSecond, 0);
                return false; // Blokir request!
            }
        }

        // ==========================================
        // JALUR 2: PROTEKSI PEMBUATAN LINK (POST /api/v1/urls)
        // ==========================================
        if (method.equals("POST") && uri.equals("/api/v1/urls")) {
            // Mengambil userId yang sebelumnya sudah dititipkan oleh JwtFilter kamu di request attribute
            Long userId = (Long) request.getAttribute("userId");

            String redisKey;
            long maxCreateLimit;

            if (userId != null) {
                // Skenario User Login: 20 per hari
                redisKey = "rate:create:user::" + userId;
                maxCreateLimit = 20;
            } else {
                // Skenario Guest: 5 per hari
                redisKey = "rate:create:guest::" + ipAddress;
                maxCreateLimit = 5;
            }

            if (!rateLimiterService.isAllowed(redisKey, maxCreateLimit, 24, TimeUnit.HOURS)) {
                long remaining = rateLimiterService.getRemainingQuota(redisKey, maxCreateLimit);
                sendManualErrorResponse(response, "Batas harian pembuatan tautan sudah habis!", maxCreateLimit, remaining);
                return false; // Blokir request!
            }
        }

        return true; // Lolos, silakan lanjut ke Controller
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * Trik Cerdas: Merakit JSON String secara manual agar bebas dari error Jackson/ObjectMapper
     */
    private void sendManualErrorResponse(HttpServletResponse response, String message, long max, long remaining) throws Exception {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String currentTimestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // Format JSON String standar yang rapi
        String jsonResponse = String.format(
                "{\"success\":false,\"message\":\"%s\",\"limitMax\":%d,\"remaining\":%d,\"timestamp\":\"%s\"}",
                message, max, remaining, currentTimestamp
        );

        PrintWriter writer = response.getWriter();
        writer.print(jsonResponse);
        writer.flush();
    }
}