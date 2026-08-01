package top.egon.cola.platform.rbac3.admin.session;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.session.application.RefreshTokenService;
import top.egon.cola.platform.rbac3.admin.session.application.SessionFacade;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionFacadeTest {

    @Test
    void loginCreatesActiveSessionWithNoImplicitRoleActivation() {
        var ids = new AtomicLong(100);
        var store = new CapturingStore();
        var facade = new SessionFacade(
                ids::incrementAndGet,
                store,
                Duration.ofMinutes(30),
                Duration.ofHours(12),
                Duration.ofDays(7));
        Instant now = Instant.parse("2026-07-30T10:00:00Z");

        var result = facade.create("200", "100", 7, 9, "device-raw", now);

        assertEquals(SessionFacade.SessionStatus.ACTIVE, result.session().status());
        assertTrue(result.session().activationRequired());
        assertEquals(0, result.session().sessionVersion());
        assertEquals(7, result.session().authVersion());
        assertEquals(9, result.session().policyVersion());
        assertNotEquals("device-raw", result.session().deviceIdHash());
        assertNotEquals(result.refreshToken(), store.refreshToken.tokenHash());
        assertTrue(result.toString().contains("<redacted>"));
    }

    private static final class CapturingStore implements SessionFacade.SessionStore {
        private SessionFacade.SessionRecord session;
        private RefreshTokenService.TokenRecord refreshToken;

        @Override
        public void create(
                SessionFacade.SessionRecord session,
                RefreshTokenService.TokenRecord refreshToken,
                Instant now) {
            this.session = session;
            this.refreshToken = refreshToken;
        }

        @Override
        public boolean logout(String tenantId, String userId, String sessionId, Instant now) {
            return true;
        }
    }
}
