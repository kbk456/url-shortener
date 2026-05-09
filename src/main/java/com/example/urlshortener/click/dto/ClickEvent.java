package com.example.urlshortener.click.dto;

import java.time.LocalDateTime;

public record ClickEvent(
        String shortCode,
        String ipAddress,
        String userAgent,
        String referer,
        LocalDateTime clickedAt
) {}
