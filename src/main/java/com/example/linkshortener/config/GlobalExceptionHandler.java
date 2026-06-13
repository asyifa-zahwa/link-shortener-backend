package com.example.linkshortener.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Menangkap IllegalArgumentException (Misal: Link tidak ditemukan, Tanggal Kedaluwarsa Gaib/Masa Lalu)
     * Mengubah status menjadi 404 NOT FOUND atau 400 BAD REQUEST (disesuaikan konteks, standardnya 404 untuk not found)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "message", ex.getMessage()
        ));
    }

    /**
     * Menangkap AccessDeniedException (Misal: User mencoba menghapus link orang lain)
     * Mengubah status menjadi 403 FORBIDDEN
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "message", ex.getMessage()
        ));
    }

    /**
     * Menangkap SecurityException (Misal: Guest mencoba membuat custom alias tanpa login)
     * Mengubah status menjadi 401 UNAUTHORIZED
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurityException(SecurityException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "success", false,
                "message", ex.getMessage()
        ));
    }

    /**
     * Menangkap error validasi data input (Gagal validasi DTO @Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "Validation failed",
                "errors", errors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Menangkap error umum lainnya (Jaring pengaman terakhir untuk Runtime Exception tidak terduga)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "An unexpected error occurred: " + ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}