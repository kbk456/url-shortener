package com.example.urlshortener.url.dto;

import java.time.LocalDateTime;

public record StatsResponse(
        String shortCode,
        String originalUrl,
        long clickCount,
        LocalDateTime createdAt,
        boolean active
) {}
