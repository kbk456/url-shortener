package com.example.urlshortener.url.dto;

import jakarta.validation.constraints.NotBlank;

public record ShortenRequest(
        @NotBlank(message = "originalUrl must not be blank")
        String originalUrl
) {}
