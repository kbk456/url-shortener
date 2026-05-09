package com.example.urlshortener.click.consumer;

import com.example.urlshortener.click.domain.UrlClickLog;
import com.example.urlshortener.click.dto.ClickEvent;
import com.example.urlshortener.click.repository.UrlClickLogRepository;
import com.example.urlshortener.url.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClickEventConsumer {

    private final UrlClickLogRepository clickLogRepository;
    private final UrlRepository urlRepository;

    @KafkaListener(topics = "url.click.events", groupId = "url-shortener-group")
    @Transactional
    public void consume(ClickEvent event) {
        log.debug("Consuming click event: shortCode={}", event.shortCode());

        UrlClickLog clickLog = UrlClickLog.builder()
                .shortCode(event.shortCode())
                .ipAddress(event.ipAddress())
                .userAgent(event.userAgent())
                .referer(event.referer())
                .clickedAt(event.clickedAt())
                .build();
        clickLogRepository.save(clickLog);

        int updated = urlRepository.incrementClickCount(event.shortCode());
        if (updated == 0) {
            log.warn("click_count increment skipped — shortCode not found: {}", event.shortCode());
        }
    }
}
