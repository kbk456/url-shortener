package com.example.urlshortener.click.producer;

import com.example.urlshortener.click.dto.ClickEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClickEventProducer {

    private static final String TOPIC = "url.click.events";

    private final KafkaTemplate<String, ClickEvent> kafkaTemplate;

    public void publish(ClickEvent event) {
        kafkaTemplate.send(TOPIC, event.shortCode(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish click event for shortCode={}", event.shortCode(), ex);
                    } else {
                        log.debug("Click event published: shortCode={}, offset={}",
                                event.shortCode(), result.getRecordMetadata().offset());
                    }
                });
    }
}
