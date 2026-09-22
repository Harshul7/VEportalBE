package org.example.veportal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Cors cors, Jwt jwt, Mail mail, Storage storage, boolean seedDemoData) {

    public record Cors(java.util.List<String> allowedOrigins) {
    }

    public record Jwt(String secret, long expirationMinutes, String cookieName, boolean cookieSecure) {
    }

    public record Storage(String materialsDir, long maxFileBytes) {
    }

    public record Mail(String host, int port, String username, String password,
                       String from, String portalUrl) {
    }
}
