package top.egon.cola.platform.rbac3.admin.session;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.application.port.Rbac3RuntimePolicy;
import top.egon.cola.platform.rbac3.admin.config.properties.Rbac3AdminProperties;
import top.egon.cola.platform.rbac3.admin.integration.ddc.AtomicRbac3RuntimePolicy;
import top.egon.cola.platform.rbac3.admin.session.service.RefreshTokenService;
import top.egon.cola.platform.rbac3.admin.session.service.SessionFacade;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.TokenRecordVO;
import top.egon.cola.platform.rbac3.admin.session.repository.SessionRepository;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.SessionRecordVO;
import top.egon.cola.platform.rbac3.admin.session.domain.enums.SessionLifecycleStatusEnum;

class SessionFacadeTest {

    @Test
    void loginCreatesActiveSessionWithNoImplicitRoleActivation() {
        var ids = new AtomicLong(100);
        var store = new CapturingStore();
        var facade = new SessionFacade(
                ids::incrementAndGet,
                store,
                new AtomicRbac3RuntimePolicy(new Rbac3AdminProperties()));
        Instant now = Instant.parse("2026-07-30T10:00:00Z");

        var result = facade.create("200", "100", 7, 9, "device-raw", now);

        assertEquals(SessionLifecycleStatusEnum.ACTIVE, result.session().status());
        assertTrue(result.session().activationRequired());
        assertEquals(0, result.session().sessionVersion());
        assertEquals(7, result.session().authVersion());
        assertEquals(9, result.session().policyVersion());
        assertNotEquals("device-raw", result.session().deviceIdHash());
        assertNotEquals(result.refreshToken(), store.refreshToken.tokenHash());
        assertTrue(result.toString().contains("<redacted>"));
    }

    @Test
    void usesOneCompleteCurrentPolicySnapshotForEachNewSession() {
        var ids = new AtomicLong(100);
        var store = new CapturingStore();
        AtomicRbac3RuntimePolicy mutablePolicy = new AtomicRbac3RuntimePolicy(
                new Rbac3AdminProperties());
        CountingPolicy policy = new CountingPolicy(mutablePolicy);
        var facade = new SessionFacade(ids::incrementAndGet, store, policy);
        Instant now = Instant.parse("2026-07-30T10:00:00Z");

        var first = facade.create("200", "100", 7, 9, "device-raw", now);
        mutablePolicy.apply(AtomicRbac3RuntimePolicy.REFRESH_TOKEN_TTL_KEY, "172800", 1L);
        mutablePolicy.apply(
                AtomicRbac3RuntimePolicy.SESSION_ABSOLUTE_TIMEOUT_KEY, "86400", 2L);
        mutablePolicy.apply(AtomicRbac3RuntimePolicy.SESSION_IDLE_TIMEOUT_KEY, "28800", 3L);
        var second = facade.create("200", "100", 7, 9, "device-raw", now);

        assertEquals(now.plus(Duration.ofMinutes(30)), first.session().idleExpiresAt());
        assertEquals(now.plus(Duration.ofHours(12)), first.session().absoluteExpiresAt());
        assertEquals(now.plus(Duration.ofDays(7)), first.refreshExpiresAt());
        assertEquals(now.plus(Duration.ofHours(8)), second.session().idleExpiresAt());
        assertEquals(now.plus(Duration.ofDays(1)), second.session().absoluteExpiresAt());
        assertEquals(now.plus(Duration.ofDays(2)), second.refreshExpiresAt());
        assertEquals(2, policy.currentCalls);
        assertEquals(first.session(), store.sessions.get(0));
        assertEquals(second.session(), store.sessions.get(1));
    }

    private static final class CapturingStore implements SessionRepository {
        private SessionRecordVO session;
        private TokenRecordVO refreshToken;
        private final List<SessionRecordVO> sessions = new ArrayList<>();

        @Override
        public void create(
                SessionRecordVO session,
                TokenRecordVO refreshToken,
                Instant now) {
            this.session = session;
            this.refreshToken = refreshToken;
            sessions.add(session);
        }

        @Override
        public boolean logout(String tenantId, String userId, String sessionId, Instant now) {
            return true;
        }
    }

    private static final class CountingPolicy implements Rbac3RuntimePolicy {

        private final Rbac3RuntimePolicy delegate;
        private int currentCalls;

        private CountingPolicy(Rbac3RuntimePolicy delegate) {
            this.delegate = delegate;
        }

        @Override
        public Snapshot current() {
            currentCalls++;
            return delegate.current();
        }
    }
}
