package top.egon.cola.component.gateway.core.security;

import top.egon.cola.component.gateway.core.context.GatewayPrincipal;

import java.util.Objects;

public record AuthenticationDecision(
        SecurityDecision decision,
        GatewayPrincipal principal,
        String reason,
        AuthenticationFailure failure
) {

    public AuthenticationDecision {
        decision = Objects.requireNonNull(decision, "decision");
        reason = reason == null || reason.isBlank() ? null : reason.trim();
        failure = Objects.requireNonNull(failure, "failure");
        if (decision == SecurityDecision.ALLOW
                && (principal == null || !principal.authenticated())) {
            throw new IllegalArgumentException(
                    "ALLOW requires an authenticated principal"
            );
        }
    }

    /**
     * Compatibility constructor for providers compiled before failure categories existed.
     */
    public AuthenticationDecision(
            SecurityDecision decision,
            GatewayPrincipal principal,
            String reason) {
        this(
                decision,
                principal,
                reason,
                decision == SecurityDecision.DENY
                        ? AuthenticationFailure.INVALID
                        : AuthenticationFailure.NONE
        );
    }

    public static AuthenticationDecision allow(
            GatewayPrincipal principal) {
        return new AuthenticationDecision(
                SecurityDecision.ALLOW,
                principal,
                null,
                AuthenticationFailure.NONE
        );
    }

    public static AuthenticationDecision deny(String reason) {
        return new AuthenticationDecision(
                SecurityDecision.DENY,
                null,
                reason,
                AuthenticationFailure.INVALID
        );
    }

    public static AuthenticationDecision abstain() {
        return new AuthenticationDecision(
                SecurityDecision.ABSTAIN,
                null,
                null,
                AuthenticationFailure.NONE
        );
    }

    public static AuthenticationDecision error(String reason) {
        return new AuthenticationDecision(
                SecurityDecision.ERROR,
                null,
                reason,
                AuthenticationFailure.NONE
        );
    }

    public static AuthenticationDecision missing(String reason) {
        return new AuthenticationDecision(
                SecurityDecision.DENY,
                null,
                reason,
                AuthenticationFailure.MISSING);
    }

    public static AuthenticationDecision expired(String reason) {
        return new AuthenticationDecision(
                SecurityDecision.DENY,
                null,
                reason,
                AuthenticationFailure.EXPIRED);
    }
}
