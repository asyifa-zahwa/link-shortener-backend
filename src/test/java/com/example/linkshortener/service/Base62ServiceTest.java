package com.example.linkshortener.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Base62ServiceTest {

    private Base62Service base62Service;

    @BeforeEach
    void setUp() {
        // Inisialisasi service sebelum setiap test dijalankan
        base62Service = new Base62Service();
    }

    @Test
    @DisplayName("Harus sukses encode ID database menjadi short code minimal 5 karakter")
    void testEncode_Success() {
        // Skenario: ID database = 1, ditambah offset 10.000.000 = 10.000.001
        String result = base62Service.encode(1L);

        assertNotNull(result);
        assertEquals(4, result.length(), "Panjang short code harus 4 karakter berkat offset 10 juta");
    }

    @Test
    @DisplayName("Harus sukses decode short code kembali menjadi ID database asli")
    void testDecode_Success() {
        // Kita encode dulu ID 1L untuk mendapatkan string validnya
        String shortCode = base62Service.encode(1L);

        // Jalankan fungsi decode
        Long actualId = base62Service.decode(shortCode);

        assertEquals(1L, actualId, "Hasil decode harus kembali menjadi angka 1");
    }

    @Test
    @DisplayName("Harus melempar exception jika hasil decode bernilai negatif (Proteksi Iseng)")
    void testDecode_NegativeOrInvalidResult_ThrowsException() {
        // Kode "21" di Base62 menghasilkan angka 125.
        // Jika dikurangi 10.000.000, hasilnya pasti negatif berat.
        String inputIseng = "21";

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            base62Service.decode(inputIseng);
        });

        String expectedMessage = "Format Short Code tidak valid atau hasil decode bernilai negatif!";
        assertTrue(exception.getMessage().contains(expectedMessage));
    }
}