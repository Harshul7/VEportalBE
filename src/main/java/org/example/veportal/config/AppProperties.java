package org.example.veportal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Cors cors, Jwt jwt, boolean seedDemoData) {

    public record Cors(java.util.List<String> allowedOrigins) {
    }

    public record Jwt(String secret, long expirationMinutes) {
    }
}
