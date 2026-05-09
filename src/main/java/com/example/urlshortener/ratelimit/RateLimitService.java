package com.example.urlshortener.ratelimit;

import com.example.urlshortener.common.config.AppProperties;
import com.example.urlshortener.common.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private static final String KEY_PREFIX = "rate-limit:";

    private final StringRedisTemplate redisTemplate;
    private final AppProperties appProperties;

    public void checkLimit(String ip) {
        String key = KEY_PREFIX + ip;
        int maxRequests = appProperties.getRateLimit().getMaxRequests();
        int windowSeconds = appProperties.getRateLimit().getWindowSeconds();

        Long count = redisTemplate.opsForValue().increment(key);
        if (Long.valueOf(1L).equals(count)) {
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
        }
        if (count != null && count > maxRequests) {
            log.debug("Rate limit exceeded for ip={}, count={}", ip, count);
            throw new RateLimitExceededException("Rate limit exceeded for ip: " + ip);
        }
    }
}
