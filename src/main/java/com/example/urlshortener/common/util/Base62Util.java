package com.example.urlshortener.common.util;

public class Base62Util {

    private static final String CHARS =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = 62;

    private Base62Util() {}

    public static String encode(long num) {
        if (num <= 0) throw new IllegalArgumentException("Input must be positive: " + num);
        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            sb.append(CHARS.charAt((int) (num % BASE)));
            num /= BASE;
        }
        return sb.reverse().toString();
    }
}
