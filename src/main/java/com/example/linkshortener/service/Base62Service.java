package com.example.linkshortener.service;

import org.springframework.stereotype.Service;

@Service
public class Base62Service {

    // Kumpulan 62 karakter sebagai bahan dasar short code
    private static final String ALLOWED_CHARACTERS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = ALLOWED_CHARACTERS.length(); // 62

    // Nilai Offset (Geser) sebesar 10 Juta agar panjang URL minimal selalu 5 karakter
    private static final long OFFSET = 10000000L;

    /**
     * Mengubah ID Asli Database menjadi String Pendek (dengan tambahan Offset)
     * Alur: ID Asli -> Ditambah 10 Juta -> Di-encode ke Base62
     */
    public String encode(Long id) {
        if (id == null || id < 1) {
            throw new IllegalArgumentException("ID database tidak valid untuk di-encode!");
        }

        // Terapkan ide kamu: Tambah dengan offset 10 juta sebelum di-encode
        long inputWithOffset = id + OFFSET;

        StringBuilder sb = new StringBuilder();

        // Algoritma pembagian sisa (modulus) berulang
        while (inputWithOffset > 0) {
            int remainder = (int) (inputWithOffset % BASE);
            sb.append(ALLOWED_CHARACTERS.charAt(remainder));
            inputWithOffset = inputWithOffset / BASE;
        }

        // Balik urutan string karena pembagian dibaca dari bawah ke atas
        return sb.reverse().toString();
    }

    /**
     * Mengubah String Pendek kembali menjadi ID Asli Database
     * Alur: Decode Base62 -> Didapat Angka Besar -> Dikurangi 10 Juta -> Validasi -> ID Asli
     */
    public Long decode(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Short code tidak boleh kosong!");
        }

        long decodedValue = 0;
        long power = 1;

        // Proses decode matematika matematika Base62 standar (berjalan mundur)
        for (int i = input.length() - 1; i >= 0; i--) {
            char c = input.charAt(i);
            int value = ALLOWED_CHARACTERS.indexOf(c);

            // Validasi jika ada karakter aneh yang tidak masuk dalam 62 karakter allowed
            if (value == -1) {
                throw new IllegalArgumentException("Karakter tidak valid di dalam Short Code: " + c);
            }

            decodedValue += value * power;
            power *= BASE;
        }

        // Terapkan ide kamu: Kurangi dengan offset 10 juta untuk mendapatkan ID aslinya
        long finalId = decodedValue - OFFSET;

        // VALIDASI PROTEKSI: Jika hasilnya nol, negatif, atau memicu angka di bawah 1
        if (finalId < 1) {
            throw new IllegalArgumentException("Format Short Code tidak valid atau hasil decode bernilai negatif!");
        }

        return finalId;
    }
}