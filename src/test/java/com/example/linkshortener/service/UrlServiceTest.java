package com.example.linkshortener.service;

import com.example.linkshortener.dto.MyUrlsResponse;
import com.example.linkshortener.dto.ShortenRequest;
import com.example.linkshortener.dto.ShortenUrlRequest;
import com.example.linkshortener.dto.ShortenUrlResponse;
import com.example.linkshortener.entity.Url;
import com.example.linkshortener.entity.User;
import com.example.linkshortener.repository.UrlRepository;
import com.example.linkshortener.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class) // Mengaktifkan fitur Mockito di JUnit 5
class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Base62Service base62Service;

    @InjectMocks
    private UrlService urlService;

    private ShortenUrlRequest requestWithAlias;
    private User dummyUser;

    @BeforeEach
    void setUp() {
        // Setup request yang membawa custom alias
        requestWithAlias = new ShortenUrlRequest();
        requestWithAlias.setLongUrl("https://www.example.com/blog/desain-sistem");
        requestWithAlias.setCustomAlias("desain-sistem");
        requestWithAlias.setExpiredAt(LocalDateTime.now().plusMonths(1));

        // Setup dummy user terdaftar
        dummyUser = new User();
        dummyUser.setId(100L);
        dummyUser.setUsername("backend_dev");
        dummyUser.setEmail("dev@pndk.id");
    }

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
    @Test
    @DisplayName("Harus sukses membuat short URL jika User terdaftar mengirimkan custom alias yang valid")
    void createShortUrl_WithValidCustomAlias_Success() {
        // GIVEN
        String currentUsername = "backend_dev";
        Mockito.when(userRepository.findByUsername(currentUsername)).thenReturn(Optional.of(dummyUser));
        Mockito.when(urlRepository.existsByShortCode("desain-sistem")).thenReturn(false);

        // Simulasi hasil simpan database
        Url savedUrl = new Url();
        savedUrl.setId(1L);
        savedUrl.setLongUrl(requestWithAlias.getLongUrl());
        savedUrl.setShortCode("desain-sistem");
        savedUrl.setExpiredAt(requestWithAlias.getExpiredAt());
        savedUrl.setUser(dummyUser);
        Mockito.when(urlRepository.save(any(Url.class))).thenReturn(savedUrl);

        // WHEN
        ShortenUrlResponse response = urlService.createShortUrl(requestWithAlias, currentUsername);

        // THEN
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("desain-sistem", response.getData().getShortCode());
        assertEquals("http://localhost:8080/desain-sistem", response.getData().getShortUrl());
        assertEquals("backend_dev", response.getData().getCreatedBy());

        // Memastikan data langsung disimpan tanpa memanggil generator acak Base62
        Mockito.verify(urlRepository, Mockito.times(1)).save(any(Url.class));
    }

    @Test
    @DisplayName("Harus melempar SecurityException jika Guest mencoba mengirimkan custom alias")
    void createShortUrl_GuestUsingAlias_ThrowsSecurityException() {
        // GIVEN: username null atau anonymousUser melambangkan Guest umum
        String currentUsername = "anonymousUser";

        // WHEN & THEN
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            urlService.createShortUrl(requestWithAlias, currentUsername);
        });

        assertEquals("Hanya user terdaftar yang boleh menggunakan fitur custom alias!", exception.getMessage());

        // Memastikan database tidak disentuh sama sekali jika melanggar hak akses
        Mockito.verify(urlRepository, Mockito.never()).existsByShortCode(any());
        Mockito.verify(urlRepository, Mockito.never()).save(any());
    }

    @Test
    @DisplayName("Harus melempar IllegalArgumentException jika User terdaftar memilih alias yang sudah terpakai")
    void createShortUrl_DuplicateAlias_ThrowsIllegalArgumentException() {
        // GIVEN
        String currentUsername = "backend_dev";
        Mockito.when(userRepository.findByUsername(currentUsername)).thenReturn(Optional.of(dummyUser));
        // Simulasi: alias 'desain-sistem' sudah ada di database
        Mockito.when(urlRepository.existsByShortCode("desain-sistem")).thenReturn(true);

        // WHEN & THEN
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            urlService.createShortUrl(requestWithAlias, currentUsername);
        });

        assertEquals("Custom alias 'desain-sistem' sudah terpakai!", exception.getMessage());

        // Memastikan proses langsung digagalkan sebelum masuk tahap menyimpan data
        Mockito.verify(urlRepository, Mockito.never()).save(any(Url.class));
    }

    @Test
    @DisplayName("Harus sukses mengembalikan daftar URL dengan paginasi jika user terdaftar mengakses")
    void getUserUrls_AuthenticatedUser_Success() {
        // GIVEN
        String currentUsername = "backend_dev";
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);

        // Membuat dummy item list URL yang berpasangan dengan total click
        MyUrlsResponse.UrlItem item1 = new MyUrlsResponse.UrlItem(
                "desain-sistem",
                "https://www.example.com/blog/desain-sistem",
                "https://pndk.id/desain-sistem",
                1420L,
                LocalDateTime.now()
        );

        MyUrlsResponse.UrlItem item2 = new MyUrlsResponse.UrlItem(
                "6bXyZ",
                "https://google.com",
                "https://pndk.id/6bXyZ",
                45L,
                LocalDateTime.now().minusDays(1)
        );

        List<MyUrlsResponse.UrlItem> content = List.of(item1, item2);
        Page<MyUrlsResponse.UrlItem> mockPage = new PageImpl<>(content, pageable, content.size());

        // Mocking fungsi repository
        Mockito.when(urlRepository.findUserUrlsWithClickCount(currentUsername, pageable)).thenReturn(mockPage);

        // WHEN
        MyUrlsResponse response = urlService.getUserUrls(currentUsername, page, size);

        // THEN
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(2, response.getData().size());

        // Verifikasi data item pertama (desain-sistem)
        assertEquals("desain-sistem", response.getData().get(0).getShortCode());
        assertEquals(1420L, response.getData().get(0).getTotalClicks());

        // Verifikasi Page Info metadata
        assertNotNull(response.getPageInfo());
        assertEquals(0, response.getPageInfo().getCurrentPage());
        assertEquals(2, response.getPageInfo().getTotalItems());
        assertEquals(1, response.getPageInfo().getTotalPages());

        // Memastikan repository dipanggil tepat 1 kali dengan parameter yang benar
        Mockito.verify(urlRepository, Mockito.times(1)).findUserUrlsWithClickCount(currentUsername, pageable);
    }

    @Test
    @DisplayName("Harus melempar SecurityException jika Guest mencoba mengakses daftar URL")
    void getUserUrls_GuestUser_ThrowsSecurityException() {
        // GIVEN: User anonim (tidak login)
        String currentUsername = "anonymousUser";
        int page = 0;
        int size = 10;

        // WHEN & THEN
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            urlService.getUserUrls(currentUsername, page, size);
        });

        assertEquals("Akses ditolak! Anda harus login untuk melihat daftar URL.", exception.getMessage());

        // Memastikan database/repository sama sekali tidak tersentuh untuk efisiensi performa
        Mockito.verify(urlRepository, Mockito.never()).findUserUrlsWithClickCount(anyString(), any(Pageable.class));
    }
}