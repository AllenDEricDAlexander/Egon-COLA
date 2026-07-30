package top.egon.cola.component.gateway.engine.security;

import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.component.gateway.core.security.GatewayCredential;
import top.egon.cola.component.gateway.core.security.TrustedIdentity;

import java.util.Objects;
import java.util.Set;

public record GatewaySecurityResult(
        GatewayAuthContext context,
        TrustedIdentity trustedIdentity,
        Set<String> fieldsToRemove,
        GatewayCredential forwardingCredential
) {

    public GatewaySecurityResult {
        context = Objects.requireNonNull(context, "context");
        trustedIdentity = Objects.requireNonNull(
                trustedIdentity,
                "trustedIdentity"
        );
        fieldsToRemove = Set.copyOf(Objects.requireNonNull(
                fieldsToRemove,
                "fieldsToRemove"
        ));
    }

    public GatewaySecurityResult(
            GatewayAuthContext context,
            TrustedIdentity trustedIdentity,
            Set<String> fieldsToRemove
    ) {
        this(context, trustedIdentity, fieldsToRemove, null);
    }

    @Override
    public String toString() {
        return "GatewaySecurityResult[context=" + context
                + ", trustedIdentity=" + trustedIdentity
                + ", fieldsToRemove=" + fieldsToRemove
                + ", forwardingCredential="
                + (forwardingCredential == null ? "NONE" : "REDACTED")
                + ']';
    }
}
