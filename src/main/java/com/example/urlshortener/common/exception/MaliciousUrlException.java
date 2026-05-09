package com.example.urlshortener.common.exception;

public class MaliciousUrlException extends RuntimeException {
    public MaliciousUrlException(String message) {
        super(message);
    }
}
