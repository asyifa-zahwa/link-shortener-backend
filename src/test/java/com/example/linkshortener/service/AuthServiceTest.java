package com.example.linkshortener.service;

import com.example.linkshortener.config.JwtUtils;
import com.example.linkshortener.dto.LoginRequest;
import com.example.linkshortener.dto.LoginResponse;
import com.example.linkshortener.dto.RegisterRequest;
import com.example.linkshortener.entity.User;
import com.example.linkshortener.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils; // <-- Tambahkan Mock JwtUtils di sini

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;
    private User dummyUser;

    @BeforeEach
    void setUp() {
        // Setup data untuk Register
        validRegisterRequest = new RegisterRequest();
        validRegisterRequest.setUsername("backend_dev");
        validRegisterRequest.setEmail("dev@pndk.id");
        validRegisterRequest.setPassword("SuperSecurePassword123!");

        // Setup data untuk Login
        validLoginRequest = new LoginRequest();
        validLoginRequest.setUsername("backend_dev");
        validLoginRequest.setPassword("SuperSecurePassword123!");

        // Setup Objek User simulasi dari Database
        dummyUser = new User();
        dummyUser.setId(1L);
        dummyUser.setUsername("backend_dev");
        dummyUser.setEmail("dev@pndk.id");
        dummyUser.setPasswordHash("hashed_password_dari_db");
    }

    // ==========================================
    // INI UNIT TEST UNTUK REGISTER (DI-MAINTAIN)
    // ==========================================

    @Test
    @DisplayName("Harus sukses mendaftarkan user baru dan mengenkripsi password")
    void registerUser_Success() {
        Mockito.when(userRepository.existsByUsername(validRegisterRequest.getUsername())).thenReturn(false);
        Mockito.when(userRepository.existsByEmail(validRegisterRequest.getEmail())).thenReturn(false);
        Mockito.when(passwordEncoder.encode(validRegisterRequest.getPassword())).thenReturn("encoded_password_hash");

        assertDoesNotThrow(() -> authService.registerUser(validRegisterRequest));

        Mockito.verify(passwordEncoder, Mockito.times(1)).encode(validRegisterRequest.getPassword());
        Mockito.verify(userRepository, Mockito.times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Harus melempar exception jika username sudah terpakai saat register")
    void registerUser_UsernameExists_ThrowsException() {
        Mockito.when(userRepository.existsByUsername(validRegisterRequest.getUsername())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.registerUser(validRegisterRequest);
        });

        assertEquals("Username sudah terdaftar!", exception.getMessage());
    }

    // ==========================================
    //  UNIT TEST TAMBAHAN UNTUK FITUR LOGIN
    // ==========================================

    @Test
    @DisplayName("Harus sukses login dan mengembalikan token JWT yang valid")
    void loginUser_Success() {
        // GIVEN
        Mockito.when(userRepository.findByUsername(validLoginRequest.getUsername())).thenReturn(Optional.of(dummyUser));
        // Simulasi: password mentah cocok dengan password hash di DB
        Mockito.when(passwordEncoder.matches(validLoginRequest.getPassword(), dummyUser.getPasswordHash())).thenReturn(true);
        // Simulasi: pabrik token sukses mencetak string token JWT
        Mockito.when(jwtUtils.generateToken(dummyUser.getUsername())).thenReturn("eyJhbGciOiJIUzI1NiI...");

        // WHEN
        LoginResponse response = authService.loginUser(validLoginRequest);

        // THEN
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("eyJhbGciOiJIUzI1NiI...", response.getAccessToken());
        assertEquals(86400, response.getExpiresIn());

        Mockito.verify(jwtUtils, Mockito.times(1)).generateToken(dummyUser.getUsername());
    }

    @Test
    @DisplayName("Harus melempar exception jika login gagal karena username tidak ditemukan")
    void loginUser_UsernameNotFound_ThrowsException() {
        // GIVEN: username palsu tidak ada di database
        Mockito.when(userRepository.findByUsername(validLoginRequest.getUsername())).thenReturn(Optional.empty());

        // WHEN & THEN
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.loginUser(validLoginRequest);
        });

        assertEquals("Username atau password salah!", exception.getMessage());
        // Memastikan proses stop, tidak nge-check password, dan tidak mencetak token
        Mockito.verify(passwordEncoder, Mockito.never()).matches(anyString(), anyString());
        Mockito.verify(jwtUtils, Mockito.never()).generateToken(anyString());
    }

    @Test
    @DisplayName("Harus melempar exception jika login gagal karena password salah")
    void loginUser_WrongPassword_ThrowsException() {
        // GIVEN: User ketemu, tapi password-nya salah saat di-matches
        Mockito.when(userRepository.findByUsername(validLoginRequest.getUsername())).thenReturn(Optional.of(dummyUser));
        Mockito.when(passwordEncoder.matches(validLoginRequest.getPassword(), dummyUser.getPasswordHash())).thenReturn(false);

        // WHEN & THEN
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.loginUser(validLoginRequest);
        });

        assertEquals("Username atau password salah!", exception.getMessage());
        // Memastikan token tidak akan pernah dicetak jika password salah
        Mockito.verify(jwtUtils, Mockito.never()).generateToken(anyString());
    }
}