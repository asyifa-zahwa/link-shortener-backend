package com.example.linkshortener.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Username tidak boleh kosong!")
    private String username;

    @NotBlank(message = "Password tidak boleh kosong!")
    private String password;
}