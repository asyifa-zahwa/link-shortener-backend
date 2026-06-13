package com.example.linkshortener.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class ShortUrlAnalyticsResponse {
    private boolean success;
    private AnalyticsData data;

    @Data
    @Builder
    public static class AnalyticsData {
        private String shortCode;
        private long totalClicks;
        private Map<String, Long> clicksByDevice;
        private Map<String, Long> clicksByBrowser;
    }
}