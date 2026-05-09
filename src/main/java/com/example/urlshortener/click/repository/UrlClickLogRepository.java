package com.example.urlshortener.click.repository;

import com.example.urlshortener.click.domain.UrlClickLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UrlClickLogRepository extends JpaRepository<UrlClickLog, Long> {

    long countByShortCode(String shortCode);
}
