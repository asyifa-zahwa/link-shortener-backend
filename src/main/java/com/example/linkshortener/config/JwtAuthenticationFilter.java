package com.example.linkshortener.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    public JwtAuthenticationFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Ambil header Authorization dari HTTP Request
        String authHeader = request.getHeader("Authorization");

        String username = null;
        String jwtToken = null;

        // 2. Cek apakah header dimulai dengan kata "Bearer "
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwtToken = authHeader.substring(7); // Potong kata "Bearer " untuk ambil token mentah
            if (jwtUtils.validateToken(jwtToken)) {
                username = jwtUtils.getUsernameFromToken(jwtToken);
            }
        }

        // 3. Jika token valid dan user belum ter-autentikasi di session saat ini
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Buat objek otentikasi internal Spring Security (sementara tanpa Roles/Authorities dulu)
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    username, null, Collections.emptyList()
            );

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Masukkan user ke dalam "Konteks Keamanan" Spring Security
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        // Lanjutkan request ke filter berikutnya (atau ke Controller)
        filterChain.doFilter(request, response);
    }
}