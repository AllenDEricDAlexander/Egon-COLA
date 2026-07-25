package top.egon.cola.component.gateway.core.security;

import top.egon.cola.component.gateway.core.context.GatewayPrincipal;

import java.util.Objects;

public record AuthenticationDecision(
        SecurityDecision decision,
        GatewayPrincipal principal,
        String reason
) {

    public AuthenticationDecision {
        decision = Objects.requireNonNull(decision, "decision");
        reason = reason == null || reason.isBlank() ? null : reason.trim();
        if (decision == SecurityDecision.ALLOW
                && (principal == null || !principal.authenticated())) {
            throw new IllegalArgumentException(
                    "ALLOW requires an authenticated principal"
            );
        }
    }

    public static AuthenticationDecision allow(
            GatewayPrincipal principal) {
        return new AuthenticationDecision(
                SecurityDecision.ALLOW,
                principal,
                null
        );
    }

    public static AuthenticationDecision deny(String reason) {
        return new AuthenticationDecision(
                SecurityDecision.DENY,
                null,
                reason
        );
    }

    public static AuthenticationDecision abstain() {
        return new AuthenticationDecision(
                SecurityDecision.ABSTAIN,
                null,
                null
        );
    }

    public static AuthenticationDecision error(String reason) {
        return new AuthenticationDecision(
                SecurityDecision.ERROR,
                null,
                reason
        );
    }
}
