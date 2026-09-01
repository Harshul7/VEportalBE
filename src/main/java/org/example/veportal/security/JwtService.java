package org.example.veportal.security;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.example.veportal.config.AppProperties;
import org.example.veportal.entity.UserAccount;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    private final byte[] secret;
    private final long expirationMinutes;
    private final ObjectMapper objectMapper;

    public JwtService(AppProperties properties, ObjectMapper objectMapper) {
        this.secret = properties.jwt().secret().getBytes(StandardCharsets.UTF_8);
        this.expirationMinutes = properties.jwt().expirationMinutes();
        this.objectMapper = objectMapper;
    }

    public String generate(UserAccount user) {
        long nowSeconds = System.currentTimeMillis() / 1000;
        long expSeconds = nowSeconds + expirationMinutes * 60;
        String payload = "{\"sub\":" + quote(user.getEmail())
                + ",\"role\":\"" + user.getRole().name() + "\""
                + ",\"iat\":" + nowSeconds
                + ",\"exp\":" + expSeconds + "}";
        String encodedHeader = base64Url(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = base64Url(payload.getBytes(StandardCharsets.UTF_8));
        String signature = sign(encodedHeader + "." + encodedPayload);
        return encodedHeader + "." + encodedPayload + "." + signature;
    }

    public String extractSubject(String token) {
        ParsedToken parsed = parseAndVerify(token);
        return parsed == null ? null : parsed.subject;
    }

    public String extractRole(String token) {
        ParsedToken parsed = parseAndVerify(token);
        return parsed == null ? null : parsed.role;
    }

    private ParsedToken parseAndVerify(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return null;
        }
        String expectedSignature = sign(parts[0] + "." + parts[1]);
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.US_ASCII),
                parts[2].getBytes(StandardCharsets.US_ASCII))) {
            return null;
        }
        try {
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonNode node = objectMapper.readTree(payloadJson);
            long exp = node.path("exp").asLong(0);
            if (exp * 1000L < System.currentTimeMillis()) {
                return null;
            }
            return new ParsedToken(node.path("sub").asText(null), node.path("role").asText(null));
        } catch (Exception e) {
            return null;
        }
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));
            return base64Url(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign JWT", e);
        }
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private record ParsedToken(String subject, String role) {
    }
}
