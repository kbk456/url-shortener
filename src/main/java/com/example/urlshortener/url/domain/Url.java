package com.example.urlshortener.url.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "url")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Url {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", length = 10)
    private String shortCode;

    @Column(name = "original_url", columnDefinition = "TEXT", nullable = false)
    private String originalUrl;

    @Column(name = "original_url_hash", length = 64, nullable = false)
    private String originalUrlHash;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "click_count", nullable = false)
    private Long clickCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void assignShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public void deactivate() {
        this.active = false;
    }
}
