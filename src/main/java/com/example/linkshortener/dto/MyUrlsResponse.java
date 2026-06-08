package com.example.linkshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MyUrlsResponse {
    private boolean success;
    private List<UrlItem> data;
    private PageInfo pageInfo;

    @Data
    @AllArgsConstructor
    public static class UrlItem {
        private String shortCode;
        private String longUrl;
        private String shortUrl;
        private long totalClicks; // Diambil dari count tabel click_analytics
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    public static class PageInfo {
        private int currentPage;
        private long totalItems;
        private int totalPages;
    }
}