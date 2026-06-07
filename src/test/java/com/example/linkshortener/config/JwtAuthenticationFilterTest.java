package com.example.linkshortener.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        // Bersihkan konteks keamanan sebelum setiap test
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        // Bersihkan kembali setelah test selesai demi keamanan thread test lain
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Harus lolos otentikasi jika Token JWT valid dan terdaftar di Header")
    void doFilterInternal_ValidToken_SetsAuthentication() throws ServletException, IOException {
        // 1. GIVEN (Simulasi Request membawa token valid)
        String mockToken = "header.payload.signature";
        String mockUsername = "backend_dev";

        request.addHeader("Authorization", "Bearer " + mockToken);

        // Atur kelakuan mock JwtUtils
        Mockito.when(jwtUtils.validateToken(mockToken)).thenReturn(true);
        Mockito.when(jwtUtils.getUsernameFromToken(mockToken)).thenReturn(mockUsername);

        // 2. WHEN (Filter dijalankan)
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // 3. THEN (Validasi hasil)
        assertNotNull(SecurityContextHolder.getContext().getAuthentication(),
                "User harusnya sukses masuk ke dalam Spring Security Context");
        assertEquals(mockUsername, SecurityContextHolder.getContext().getAuthentication().getName(),
                "Username yang terdaftar di sistem harus sesuai dengan isi token");

        // Memastikan request diteruskan ke filter/controller selanjutnya
        Mockito.verify(filterChain, Mockito.times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Harus mengabaikan otentikasi jika request tidak membawa header Authorization (Skenario Guest)")
    void doFilterInternal_NoToken_ProceedsWithoutAuthentication() throws ServletException, IOException {
        // 1. GIVEN (Request polosan tanpa header Authorization)

        // 2. WHEN
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // 3. THEN
        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "Konteks keamanan harus tetap kosong/null karena tidak ada token");

        // JwtUtils tidak boleh dipanggil sama sekali karena tidak ada token yang perlu divalidasi
        Mockito.verify(jwtUtils, Mockito.never()).validateToken(anyString());
        // Request harus tetap lolos diteruskan ke filter berikutnya (misal diarahkan ke rute guest)
        Mockito.verify(filterChain, Mockito.times(1)).doFilter(request, response);
    }
}