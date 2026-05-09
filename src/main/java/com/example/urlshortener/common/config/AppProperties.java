package com.example.urlshortener.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private String baseUrl;
    private Cache cache = new Cache();
    private RateLimit rateLimit = new RateLimit();
    private Blacklist blacklist = new Blacklist();

    @Getter
    @Setter
    public static class Cache {
        private int defaultTtlDays = 7;
    }

    @Getter
    @Setter
    public static class RateLimit {
        private int maxRequests = 10;
        private int windowSeconds = 60;
    }

    @Getter
    @Setter
    public static class Blacklist {
        private List<String> domains = new ArrayList<>();
    }
}
