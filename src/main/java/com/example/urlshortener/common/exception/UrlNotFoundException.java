package com.example.urlshortener.common.exception;

public class UrlNotFoundException extends RuntimeException {
    public UrlNotFoundException(String shortCode) {
        super("URL not found: " + shortCode);
    }
}
