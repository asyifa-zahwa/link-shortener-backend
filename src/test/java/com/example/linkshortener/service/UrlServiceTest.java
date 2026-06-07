package com.example.linkshortener.service;

import com.example.linkshortener.dto.ShortenRequest;
import com.example.linkshortener.entity.Url;
import com.example.linkshortener.repository.UrlRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class) // Mengaktifkan fitur Mockito di JUnit 5
class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository; // Membuat tiruan dari repository

    @Mock
    private Base62Service base62Service; // Membuat tiruan dari Base62Service

    @InjectMocks
    private UrlService urlService; // Otomatis menyuntikkan mock di atas ke dalam UrlService

    @Test
    @DisplayName("Guest harus sukses membuat link pendek otomatis")
    void createGuestUrl_Success() {
        // 1. GIVEN (Menyiapkan data simulasi)
        ShortenRequest request = new ShortenRequest();
        request.setLongUrl("https://google.com");

        Url dummySavedUrl = new Url();
        dummySavedUrl.setId(1L); // Simulasikan seolah-olah Postgres sukses memberikan ID 1
        dummySavedUrl.setLongUrl(request.getLongUrl());

        // Mengatur kelakuan Mock: jika repository.save() dipanggil, kembalikan dummySavedUrl
        Mockito.when(urlRepository.save(any(Url.class))).thenReturn(dummySavedUrl);

        // Mengatur kelakuan Mock: jika base62Service.encode(1L) dipanggil, kembalikan "FX9N"
        Mockito.when(base62Service.encode(1L)).thenReturn("FX9N");

        // 2. WHEN (Menjalankan fungsi yang mau diuji)
        String resultShortCode = urlService.createGuestUrl(request);

        // 3. THEN (Memastikan hasilnya sesuai ekspektasi)
        assertNotNull(resultShortCode);
        assertEquals("FX9N", resultShortCode);

        // Memastikan repository.save() dipanggil sebanyak 2 kali (insert awal dan update short_code)
        Mockito.verify(urlRepository, Mockito.times(2)).save(any(Url.class));
    }
}