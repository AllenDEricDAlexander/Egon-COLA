package top.egon.cola.platform.rbac3.admin.session.application;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.application.port.AuditPort;
import top.egon.cola.platform.rbac3.admin.application.port.AuthorizationEventPort;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.TerminationVO;
import top.egon.cola.platform.rbac3.admin.session.service.SessionSecurityEventRecorder;

class SessionSecurityEventRecorderTest {

    @Test
    void recordsTheAuditAndReliableRevocationEventFromOneCommittedFact() {
        AtomicReference<AuditPort.AuditEvent> audit = new AtomicReference<>();
        AtomicReference<AuthorizationEventPort.AuthorizationEvent> event =
                new AtomicReference<>();
        var recorder = new SessionSecurityEventRecorder(
                audit::set,
                value -> {
                    event.set(value);
                    return "outbox-1";
                });
        Instant now = Instant.parse("2026-08-01T08:00:00Z");

        recorder.record(new TerminationVO(
                "10", "20", "30", 7, "COMPROMISED",
                "REFRESH_TOKEN_REUSED", "refresh-replay", now));

        assertThat(audit.get().eventType()).isEqualTo("REFRESH_TOKEN_REPLAY_DETECTED");
        assertThat(audit.get().outcome()).isEqualTo("DENIED");
        assertThat(audit.get().reasonCode()).isEqualTo("REFRESH_TOKEN_REUSED");
        assertThat(audit.get().targetType()).isEqualTo("SESSION");
        assertThat(audit.get().safeEvidence())
                .containsEntry("reason", "REFRESH_TOKEN_REUSED")
                .containsEntry("sessionVersion", "7")
                .doesNotContainKeys("token", "refreshToken", "tokenHash");
        assertThat(event.get().eventType()).isEqualTo("SESSION_REVOKED");
        assertThat(event.get().safePayload())
                .containsEntry("userId", "20")
                .containsEntry("status", "COMPROMISED")
                .containsEntry("reason", "REFRESH_TOKEN_REUSED")
                .containsEntry("sessionVersion", "7");
    }
}
