package com.example.linkshortener.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Username tidak boleh kosong!")
    @Size(min = 3, max = 50, message = "Username minimal 3 sampai 50 karakter")
    private String username;

    @NotBlank(message = "Email tidak boleh kosong!")
    @Email(message = "Format email tidak valid!")
    private String email;

    @NotBlank(message = "Password tidak boleh kosong!")
    @Size(min = 6, message = "Password minimal terdiri dari 6 karakter")
    private String password;
}