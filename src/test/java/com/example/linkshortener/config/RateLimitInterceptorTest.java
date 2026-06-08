package com.example.linkshortener.config;

import com.example.linkshortener.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class RateLimitInterceptorTest {

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private RateLimitInterceptor rateLimitInterceptor;

    private StringWriter responseOutput;

    @BeforeEach
    void setUp() throws Exception {
        // Siapkan writer tiruan untuk menangkap JSON output saat kena rate limit
        responseOutput = new StringWriter();
        PrintWriter printWriter = new PrintWriter(responseOutput);
        Mockito.lenient().when(response.getWriter()).thenReturn(printWriter);
    }

    @Test
    @DisplayName("Harus Lolos (Return True) Jika Klik Link Belum Melewati Batas Limit")
    void preHandle_ClickLink_Allowed() throws Exception {
        // GIVEN: Menembak rute pengalihan pendek publik /FX9N via GET
        Mockito.when(request.getRequestURI()).thenReturn("/FX9N");
        Mockito.when(request.getMethod()).thenReturn("GET");
        Mockito.when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        // Simulasikan Redis menetapkan bahwa request ini MASIH DIIZINKAN (true)
        Mockito.when(rateLimiterService.isAllowed(anyString(), eq(3L), eq(1L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);

        // WHEN
        boolean result = rateLimitInterceptor.preHandle(request, response, new Object());

        // THEN
        assertTrue(result, "Interceptor harus mengembalikan TRUE jika kuota masih ada");

        // Memastikan tidak ada pengiriman error status 429 karena lolos
        Mockito.verify(response, Mockito.never()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    @DisplayName("Harus Dicegat (Return False & Status 429) Jika Klik Link Sudah Melebihi Batas 3 Klik/Detik")
    void preHandle_ClickLink_Blocked() throws Exception {
        // GIVEN: Menembak rute yang sama /FX9N via GET secara brutal
        Mockito.when(request.getRequestURI()).thenReturn("/FX9N");
        Mockito.when(request.getMethod()).thenReturn("GET");
        Mockito.when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        // Simulasikan Redis menetapkan bahwa request ini SUDAH MELAMPAUI LIMIT (false)
        Mockito.when(rateLimiterService.isAllowed(anyString(), eq(3L), eq(1L), eq(TimeUnit.SECONDS)))
                .thenReturn(false);

        // WHEN
        boolean result = rateLimitInterceptor.preHandle(request, response, new Object());

        // THEN
        assertFalse(result, "Interceptor harus mengembalikan FALSE untuk memotong request!");

        // Memastikan interseptor memasang HTTP Status 429 Too Many Requests
        Mockito.verify(response, Mockito.times(1)).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        Mockito.verify(response, Mockito.times(1)).setContentType("application/json");

        // Memastikan isi response berisi JSON error buatan manual kita kemarin
        String jsonResult = responseOutput.toString();
        assertTrue(jsonResult.contains("\"success\":false"));
        assertTrue(jsonResult.contains("kamu mengeklik tautan terlalu cepat"));
    }

    @Test
    @DisplayName("Harus Lolos Pembuatan Link Untuk User Terdaftar Jika Kuota Harian Masih Ada")
    void preHandle_CreateLink_User_Allowed() throws Exception {
        // GIVEN: Menembak rute POST pembuatan link
        Mockito.when(request.getRequestURI()).thenReturn("/api/v1/urls");
        Mockito.when(request.getMethod()).thenReturn("POST");

        // Simulasikan Filter JWT kemarin sukses menitipkan userId angka 99L di request attribute
        Mockito.when(request.getAttribute("userId")).thenReturn(99L);

        // Simulasikan kuota 20 per hari milik User masih aman di Redis
        Mockito.when(rateLimiterService.isAllowed(contains("rate:create:user::99"), eq(20L), eq(24L), eq(TimeUnit.HOURS)))
                .thenReturn(true);

        // WHEN
        boolean result = rateLimitInterceptor.preHandle(request, response, new Object());

        // THEN
        assertTrue(result);
        Mockito.verify(response, Mockito.never()).setStatus(anyInt());
    }
}