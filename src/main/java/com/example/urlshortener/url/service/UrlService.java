package com.example.urlshortener.url.service;

import com.example.urlshortener.cache.UrlCacheService;
import com.example.urlshortener.cache.dto.UrlCacheValue;
import com.example.urlshortener.click.dto.ClickEvent;
import com.example.urlshortener.click.producer.ClickEventProducer;
import com.example.urlshortener.common.config.AppProperties;
import com.example.urlshortener.common.exception.MaliciousUrlException;
import com.example.urlshortener.common.exception.UrlNotFoundException;
import com.example.urlshortener.common.util.Base62Util;
import com.example.urlshortener.url.domain.Url;
import com.example.urlshortener.url.dto.ShortenRequest;
import com.example.urlshortener.url.dto.ShortenResponse;
import com.example.urlshortener.url.dto.StatsResponse;
import com.example.urlshortener.url.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlService {

    private final UrlRepository urlRepository;
    private final UrlCacheService cacheService;
    private final ClickEventProducer clickEventProducer;
    private final AppProperties appProperties;

    @Transactional
    public ShortenResponse shorten(ShortenRequest request) {
        validateNotMalicious(request.originalUrl());

        String hash = hashUrl(request.originalUrl());

        Optional<Url> existing = urlRepository.findByOriginalUrlHashAndActiveTrue(hash);
        if (existing.isPresent()) {
            log.debug("Duplicate URL detected, returning existing shortCode={}", existing.get().getShortCode());
            return toResponse(existing.get());
        }

        Url url = Url.builder()
                .originalUrl(request.originalUrl())
                .originalUrlHash(hash)
                .active(true)
                .clickCount(0L)
                .createdAt(LocalDateTime.now())
                .build();

        urlRepository.save(url);
        urlRepository.flush();

        String shortCode = Base62Util.encode(url.getId());
        url.assignShortCode(shortCode);
        urlRepository.save(url);

        cacheService.put(shortCode, new UrlCacheValue(url.getId(), url.getOriginalUrl()));

        log.debug("Created shortCode={} for url={}", shortCode, request.originalUrl());
        return toResponse(url);
    }

    // Hot path — keep as light as possible
    public String resolveOriginalUrl(String shortCode, String ipAddress, String userAgent, String referer) {
        Optional<UrlCacheValue> cached = cacheService.get(shortCode);

        String originalUrl;
        if (cached.isPresent()) {
            originalUrl = cached.get().originalUrl();
        } else {
            Url url = urlRepository.findByShortCodeAndActiveTrue(shortCode)
                    .orElseThrow(() -> new UrlNotFoundException(shortCode));
            cacheService.put(shortCode, new UrlCacheValue(url.getId(), url.getOriginalUrl()));
            originalUrl = url.getOriginalUrl();
        }

        clickEventProducer.publish(new ClickEvent(shortCode, ipAddress, userAgent, referer, LocalDateTime.now()));

        return originalUrl;
    }

    @Transactional(readOnly = true)
    public StatsResponse getStats(String shortCode) {
        Url url = urlRepository.findByShortCodeAndActiveTrue(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));
        return new StatsResponse(
                url.getShortCode(),
                url.getOriginalUrl(),
                url.getClickCount(),
                url.getCreatedAt(),
                url.isActive()
        );
    }

    private void validateNotMalicious(String originalUrl) {
        try {
            URI uri = new URI(originalUrl);
            String host = uri.getHost();
            if (host == null) {
                throw new MaliciousUrlException("Invalid URL: missing host");
            }
            List<String> blacklist = appProperties.getBlacklist().getDomains();
            for (String blocked : blacklist) {
                if (host.equals(blocked) || host.endsWith("." + blocked)) {
                    throw new MaliciousUrlException("Blocked domain: " + host);
                }
            }
        } catch (URISyntaxException e) {
            throw new MaliciousUrlException("Invalid URL format: " + originalUrl);
        }
    }

    private String hashUrl(String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(url.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private ShortenResponse toResponse(Url url) {
        String shortUrl = appProperties.getBaseUrl() + "/" + url.getShortCode();
        return new ShortenResponse(
                url.getShortCode(),
                shortUrl,
                url.getOriginalUrl(),
                url.getCreatedAt()
        );
    }
}
