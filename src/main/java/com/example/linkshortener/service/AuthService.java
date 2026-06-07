package com.example.linkshortener.service;

import com.example.linkshortener.config.JwtUtils;
import com.example.linkshortener.dto.LoginRequest;
import com.example.linkshortener.dto.LoginResponse;
import com.example.linkshortener.dto.RegisterRequest;
import com.example.linkshortener.entity.User;
import com.example.linkshortener.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

//    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
//        this.userRepository = userRepository;
//        this.passwordEncoder = passwordEncoder;
//    }
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @Transactional(readOnly = true)
    public LoginResponse loginUser(LoginRequest request) {
        // 1. Cari user berdasarkan username
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Username atau password salah!"));

        // 2. Cocokkan password mentah dengan password terenkripsi di database
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Username atau password salah!");
        }

        // 3. Generate Token JWT
        String token = jwtUtils.generateToken(user.getUsername());

        // 4. Kembalikan data sesuai kontrak (expiresIn: 86400 detik = 24 jam)
        return new LoginResponse(true, "Bearer", token, 86400);
    }

    @Transactional
    public void registerUser(RegisterRequest request) {
        // 1. Validasi keunikan username
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username sudah terdaftar!");
        }

        // 2. Validasi keunikan email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email sudah terdaftar!");
        }

        // 3. Mapping DTO ke Entity & Hashing Password
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        // Enkripsi password sebelum disimpan ke database
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        user.setPasswordHash(hashedPassword);

        // 4. Simpan ke database
        userRepository.save(user);
    }
}