package com.example.linkshortener.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ua_parser.Parser;

@Configuration
public class AppConfig {

    /**
     * Daftarkan objek Parser dari library uap-java ke dalam IoC Container Spring.
     * Dengan begini, Spring Boot bisa menyuntikkannya ke constructor AnalyticsService tanpa error.
     */
    @Bean
    public Parser uaParser() {
        return new Parser();
    }
}