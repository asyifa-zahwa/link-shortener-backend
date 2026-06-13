package com.example.linkshortener.scheduler;

import com.example.linkshortener.repository.UrlRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Component
public class UrlCleanupScheduler {

    private final UrlRepository urlRepository;

    public UrlCleanupScheduler(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }

    /**
     * Eksekusi otomatis menggunakan ekspresi Cron Job.
     * "0 0 2 * * ?" artinya: Tugas ini akan berjalan setiap hari pada jam 02:00 Pagi.
     * Jam 2 pagi dipilih karena umumnya trafik aplikasi sedang sepi-sepinya,
     * sehingga proses DELETE massal tidak akan mengganggu performa user.
     */
    @Transactional
    @Scheduled(cron = "0 0 2 * * ?")
//    @Scheduled(fixedRate = 10000) // Ubah sementara untuk testing
    public void deleteExpiredUrlsDaily() {
        System.out.println("====== [SCHEDULER START] Memulai proses pembersihan link kedaluwarsa... ======");

        LocalDateTime now = LocalDateTime.now();

        try {
            urlRepository.deleteByExpiredAtBefore(now);
            System.out.println("====== [SCHEDULER SUCCESS] Sampah data link berhasil disapu bersih! ======");
        } catch (Exception e) {
            System.err.println("====== [SCHEDULER ERROR] Gagal membersihkan database: " + e.getMessage() + " ======");
        }
    }
}