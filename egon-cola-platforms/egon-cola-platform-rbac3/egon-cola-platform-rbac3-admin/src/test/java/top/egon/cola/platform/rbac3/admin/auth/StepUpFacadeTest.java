package top.egon.cola.platform.rbac3.admin.auth;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.auth.service.IdentityAuthenticatorStrategy;
import top.egon.cola.platform.rbac3.admin.auth.service.StepUpFacade;
import top.egon.cola.platform.rbac3.contract.auth.LoginRequest;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.AuthenticatedIdentityVO;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.StepUpIdentityVO;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.StepUpResultVO;

class StepUpFacadeTest {

    private static final Instant NOW = Instant.parse("2026-08-01T03:00:00Z");

    @Test
    void reauthenticatesAndStrengthensOnlyTheCurrentSession() {
        AtomicReference<LoginRequest> request = new AtomicReference<>();
        AtomicReference<String> strengthenedSession = new AtomicReference<>();
        StepUpFacade facade = new StepUpFacade(
                (login, now) -> {
                    request.set(login);
                    return new AuthenticatedIdentityVO(
                            "10", "20", "PASSWORD", 1);
                },
                (tenantId, userId) -> new StepUpIdentityVO("tenant", "mario"),
                (tenantId, userId, sessionId, now) -> {
                    strengthenedSession.set(sessionId);
                    return new StepUpResultVO(sessionId, "STRONG", now);
                });

        StepUpResultVO result = facade.stepUp(
                "10", "20", "30", " password ", "never-log", NOW);

        assertThat(result.sessionId()).isEqualTo("30");
        assertThat(strengthenedSession).hasValue("30");
        assertThat(request.get().tenantCode()).isEqualTo("tenant");
        assertThat(request.get().username()).isEqualTo("mario");
        assertThat(request.get().password()).isEqualTo("never-log");
        assertThat(request.get().toString()).doesNotContain("never-log");
    }

    @Test
    void rejectsUnsupportedMethodAndMismatchedAuthenticatedIdentity() {
        StepUpFacade unsupported = new StepUpFacade(
                (request, now) -> null,
                (tenantId, userId) -> new StepUpIdentityVO("tenant", "mario"),
                (tenantId, userId, sessionId, now) -> null);
        assertThatThrownBy(() -> unsupported.stepUp(
                "10", "20", "30", "TOTP", "secret", NOW))
                .isInstanceOf(Rbac3RuleViolation.class)
                .hasMessageContaining("STEP_UP_METHOD_UNSUPPORTED");

        StepUpFacade mismatch = new StepUpFacade(
                (request, now) -> new AuthenticatedIdentityVO(
                        "other-tenant", "20", "PASSWORD", 1),
                (tenantId, userId) -> new StepUpIdentityVO("tenant", "mario"),
                (tenantId, userId, sessionId, now) -> null);
        assertThatThrownBy(() -> mismatch.stepUp(
                "10", "20", "30", "PASSWORD", "secret", NOW))
                .isInstanceOf(Rbac3RuleViolation.class)
                .hasMessageContaining("AUTHENTICATION_FAILED");
    }
}
