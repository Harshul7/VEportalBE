package org.example.veportal.security;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

/** Lightweight node-local login throttling; production deployments should back this with a shared store. */
@Service
public class LoginAttemptService {
    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private final ConcurrentMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        Attempt attempt = attempts.get(normalize(key));
        if (attempt == null) return false;
        if (attempt.lockedUntil != null && attempt.lockedUntil.isAfter(Instant.now())) return true;
        if (attempt.started.plus(WINDOW).isBefore(Instant.now())) {
            attempts.remove(normalize(key), attempt);
            return false;
        }
        return false;
    }

    public void failed(String key) {
        String normalized = normalize(key);
        attempts.compute(normalized, (ignored, current) -> {
            Instant now = Instant.now();
            Attempt value = current == null || current.started.plus(WINDOW).isBefore(now)
                    ? new Attempt(now, 0, null) : current;
            int failures = value.failures + 1;
            return new Attempt(value.started, failures,
                    failures >= MAX_FAILURES ? now.plus(WINDOW) : value.lockedUntil);
        });
    }

    public void succeeded(String key) { attempts.remove(normalize(key)); }

    private String normalize(String key) { return key == null ? "" : key.trim().toLowerCase(); }

    private record Attempt(Instant started, int failures, Instant lockedUntil) {}
}
