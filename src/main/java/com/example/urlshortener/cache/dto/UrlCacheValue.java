package com.example.urlshortener.cache.dto;

public record UrlCacheValue(
        Long urlId,
        String originalUrl
) {}
