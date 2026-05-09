package com.example.urlshortener.url.controller;

import com.example.urlshortener.url.dto.ShortenRequest;
import com.example.urlshortener.url.dto.ShortenResponse;
import com.example.urlshortener.url.dto.StatsResponse;
import com.example.urlshortener.url.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;

    @PostMapping("/api/shorten")
    public ResponseEntity<ShortenResponse> shorten(@RequestBody @Valid ShortenRequest request) {
        return ResponseEntity.ok(urlService.shorten(request));
    }

    // Hot path: 302 redirect
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode,
                                         HttpServletRequest request) {
        String originalUrl = urlService.resolveOriginalUrl(
                shortCode,
                resolveClientIp(request),
                request.getHeader("User-Agent"),
                request.getHeader("Referer")
        );
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(originalUrl))
                .build();
    }

    @GetMapping("/api/stats/{shortCode}")
    public ResponseEntity<StatsResponse> stats(@PathVariable String shortCode) {
        return ResponseEntity.ok(urlService.getStats(shortCode));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
