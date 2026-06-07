package com.example.linkshortener.service;

import com.example.linkshortener.dto.ShortenRequest;
import com.example.linkshortener.entity.Url;
import com.example.linkshortener.repository.UrlRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UrlService {

    private final UrlRepository urlRepository;
    private final Base62Service base62Service;

    // Dependency Injection melalui Constructor
    public UrlService(UrlRepository urlRepository, Base62Service base62Service) {
        this.urlRepository = urlRepository;
        this.base62Service = base62Service;
    }

    /**
     * Logika untuk memperpendek URL dari Guest (Tanpa Login)
     */
    @Transactional
    public String createGuestUrl(ShortenRequest request) {
        // 1. Buat object Entity Url baru
        Url url = new Url();
        url.setLongUrl(request.getLongUrl());
        url.setUser(null); // NULL karena dibuat oleh Guest

        // 2. Simpan dulu ke database PostgreSQL untuk memicu dan mendapatkan ID Auto-Increment (BIGSERIAL)
        Url savedUrl = urlRepository.save(url);

        // 3. Ambil ID tersebut, lalu masukkan ke Base62Service dengan trik offset 10 juta idemumu
        String shortCode = base62Service.encode(savedUrl.getId());

        // 4. Update data Url tadi dengan memasukkan hasil shortCode-nya
        savedUrl.setShortCode(shortCode);
        urlRepository.save(savedUrl);

        // 5. Kembalikan kode pendeknya ke Controller
        return shortCode;
    }
}