package com.example.linkshortener.service;

import com.example.linkshortener.entity.Url;
import com.example.linkshortener.repository.UrlRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class UrlRedirectServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private AnalyticsService analyticsService; // <-- Sekarang kita meniru AnalyticsService, bukan ClickAnalyticsRepository

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private UrlRedirectService urlRedirectService;

    @Test
    @DisplayName("Harus sukses mengambil URL asli dan memicu fungsi analitik async")
    void getOriginalUrlAndRecordClick_Success() {
        // 1. GIVEN
        String shortCode = "FX9N";

        Url dummyUrl = new Url();
        dummyUrl.setId(12L);
        dummyUrl.setShortCode(shortCode);
        dummyUrl.setLongUrl("https://google.com");
        dummyUrl.setExpiredAt(LocalDateTime.now().plusDays(7));

        Mockito.when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(dummyUrl));

        // Simulasi data request browser
        Mockito.when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");
        Mockito.when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 Chrome/120.0");
        Mockito.when(request.getHeader("Referer")).thenReturn("https://twitter.com");

        // 2. WHEN
        String originalUrl = urlRedirectService.getOriginalUrlAndRecordClick(shortCode, request);

        // 3. THEN
        assertNotNull(originalUrl);
        assertEquals("https://google.com", originalUrl);

        // MEMBUKTIKAN ASYNC BERJALAN:
        // Verifikasi bahwa UrlRedirectService benar-benar menyuruh AnalyticsService untuk berjalan 1x
        Mockito.verify(analyticsService, Mockito.times(1))
                .recordClickAnalyticsAsync(
                        eq(dummyUrl),
                        eq("192.168.1.1"),
                        eq("Mozilla/5.0 Chrome/120.0"),
                        eq("https://twitter.com")
                );
    }

    @Test
    @DisplayName("Harus melempar exception jika short code tidak ditemukan dan tidak memicu analitik")
    void getOriginalUrlAndRecordClick_NotFound_ThrowsException() {
        // 1. GIVEN
        String kodePalsu = "Zzzz";
        Mockito.when(urlRepository.findByShortCode(kodePalsu)).thenReturn(Optional.empty());

        // 2. WHEN & THEN
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            urlRedirectService.getOriginalUrlAndRecordClick(kodePalsu, request);
        });

        assertEquals("Link pendek tidak ditemukan!", exception.getMessage());

        // Memastikan jika link tidak ada, kurir Async TIDAK BOLEH dipanggil sama sekali
        Mockito.verify(analyticsService, Mockito.never())
                .recordClickAnalyticsAsync(any(), any(), any(), any());
    }
}