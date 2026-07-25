package top.egon.cola.component.gateway.core.security;

import java.util.Objects;

public record AuthorizationDecision(
        SecurityDecision decision,
        String reason
) {

    public AuthorizationDecision {
        decision = Objects.requireNonNull(decision, "decision");
        reason = reason == null || reason.isBlank() ? null : reason.trim();
    }

    public static AuthorizationDecision allow() {
        return new AuthorizationDecision(SecurityDecision.ALLOW, null);
    }

    public static AuthorizationDecision deny(String reason) {
        return new AuthorizationDecision(
                SecurityDecision.DENY,
                reason
        );
    }

    public static AuthorizationDecision abstain() {
        return new AuthorizationDecision(
                SecurityDecision.ABSTAIN,
                null
        );
    }

    public static AuthorizationDecision error(String reason) {
        return new AuthorizationDecision(
                SecurityDecision.ERROR,
                reason
        );
    }
}
