package com.example.urlshortener.cache;

import com.example.urlshortener.cache.dto.UrlCacheValue;
import com.example.urlshortener.common.config.AppProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlCacheService {

    private static final String KEY_PREFIX = "short-url:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    public Optional<UrlCacheValue> get(String shortCode) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + shortCode);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, UrlCacheValue.class));
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize cache for shortCode={}", shortCode, e);
            return Optional.empty();
        }
    }

    public void put(String shortCode, UrlCacheValue value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            Duration ttl = Duration.ofDays(appProperties.getCache().getDefaultTtlDays());
            redisTemplate.opsForValue().set(KEY_PREFIX + shortCode, json, ttl);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize cache for shortCode={}", shortCode, e);
        }
    }

    public void evict(String shortCode) {
        redisTemplate.delete(KEY_PREFIX + shortCode);
    }
}
