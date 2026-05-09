package com.example.urlshortener.url.dto;

import java.time.LocalDateTime;

public record ShortenResponse(
        String shortCode,
        String shortUrl,
        String originalUrl,
        LocalDateTime createdAt
) {}
