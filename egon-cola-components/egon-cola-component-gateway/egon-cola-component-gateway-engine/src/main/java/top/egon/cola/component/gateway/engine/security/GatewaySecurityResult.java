package top.egon.cola.component.gateway.engine.security;

import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.component.gateway.core.security.TrustedIdentity;

import java.util.Objects;
import java.util.Set;

public record GatewaySecurityResult(
        GatewayAuthContext context,
        TrustedIdentity trustedIdentity,
        Set<String> fieldsToRemove
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
}
