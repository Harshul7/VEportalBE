package org.example.veportal.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LoginAttemptServiceTest {

    @Test
    void blocksAfterFiveFailuresAndClearsAfterSuccess() {
        LoginAttemptService service = new LoginAttemptService();

        for (int i = 0; i < 4; i++) {
            service.failed("faculty@example.com");
            assertFalse(service.isBlocked("faculty@example.com"));
        }
        service.failed("faculty@example.com");
        assertTrue(service.isBlocked("FACULTY@EXAMPLE.COM"));

        service.succeeded("faculty@example.com");
        assertFalse(service.isBlocked("faculty@example.com"));
    }
}
