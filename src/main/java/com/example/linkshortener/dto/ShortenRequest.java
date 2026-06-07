package com.example.linkshortener.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;
import lombok.Data;

@Data
public class ShortenRequest {

    @NotBlank(message = "URL asal tidak boleh kosong!")
    @URL(message = "Format URL tidak valid! Harus diawali http:// atau https://")
    private String longUrl;
}