package top.egon.cola.platform.rbac3.admin.session.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SessionStepUpTest {

    private static final Instant AUTHENTICATED_AT = Instant.parse("2026-08-01T01:00:00Z");

    @Test
    void recordsRecentStrongAuthenticationWithoutChangingAuthorizationVersion() {
        SessionEntity session = session();
        Instant steppedUpAt = AUTHENTICATED_AT.plusSeconds(300);

        session.stepUp("7", steppedUpAt);

        assertThat(session.getAuthenticationStrength())
                .isEqualTo(SessionEntity.AuthenticationStrength.STRONG);
        assertThat(session.getStrongAuthenticatedAt()).isEqualTo(steppedUpAt);
        assertThat(session.getSessionVersion()).isZero();
        assertThat(session.isStrongAuthenticationRecent(
                steppedUpAt.plusSeconds(599), java.time.Duration.ofMinutes(10)))
                .isTrue();
        assertThat(session.isStrongAuthenticationRecent(
                steppedUpAt.plusSeconds(600), java.time.Duration.ofMinutes(10)))
                .isFalse();
    }

    private SessionEntity session() {
        return new SessionEntity(
                1L, 2L, 3L, 4L, 0L, 0L, "family", "device",
                SessionEntity.AuthenticationStrength.PASSWORD,
                AUTHENTICATED_AT, AUTHENTICATED_AT.plusSeconds(1_800),
                AUTHENTICATED_AT.plusSeconds(3_600), "3");
    }
}
