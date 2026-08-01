package top.egon.cola.platform.rbac3.admin.auth.application;

import top.egon.cola.platform.rbac3.contract.auth.LoginRequest;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/** Re-authenticates the current identity and strengthens only its current session. */
public final class StepUpFacade {

    private final IdentityAuthenticatorStrategy authenticator;
    private final IdentitySource identitySource;
    private final SessionStrengthStore sessionStore;

    public StepUpFacade(
            IdentityAuthenticatorStrategy authenticator,
            IdentitySource identitySource,
            SessionStrengthStore sessionStore) {
        this.authenticator = Objects.requireNonNull(authenticator, "authenticator");
        this.identitySource = Objects.requireNonNull(identitySource, "identitySource");
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
    }

    public StepUpResult stepUp(
            String tenantId,
            String userId,
            String sessionId,
            String method,
            String credential,
            Instant now) {
        if (!"PASSWORD".equals(normalizeMethod(method))) {
            throw new Rbac3RuleViolation("STEP_UP_METHOD_UNSUPPORTED");
        }
        Identity identity = identitySource.load(tenantId, userId);
        IdentityAuthenticatorStrategy.AuthenticatedIdentity authenticated =
                authenticator.authenticate(new LoginRequest(
                        identity.tenantCode(), identity.username(), credential,
                        new LoginRequest.Device("step-up:" + sessionId, "Current session")),
                        now);
        if (!tenantId.equals(authenticated.tenantId())
                || !userId.equals(authenticated.userId())) {
            throw new Rbac3RuleViolation("AUTHENTICATION_FAILED");
        }
        return sessionStore.strengthen(tenantId, userId, sessionId, now);
    }

    private static String normalizeMethod(String method) {
        return method == null ? "" : method.trim().toUpperCase(Locale.ROOT);
    }

    @FunctionalInterface
    public interface IdentitySource {

        Identity load(String tenantId, String userId);
    }

    @FunctionalInterface
    public interface SessionStrengthStore {

        StepUpResult strengthen(
                String tenantId, String userId, String sessionId, Instant now);
    }

    public record Identity(String tenantCode, String username) {

        public Identity {
            tenantCode = required(tenantCode, "tenantCode");
            username = required(username, "username");
        }
    }

    public record StepUpResult(
            String sessionId,
            String authStrength,
            Instant strongAuthenticatedAt
    ) {
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
