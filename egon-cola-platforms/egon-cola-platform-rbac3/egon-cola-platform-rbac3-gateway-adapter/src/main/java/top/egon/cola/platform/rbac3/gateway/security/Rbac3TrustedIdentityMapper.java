package top.egon.cola.platform.rbac3.gateway.security;

import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.core.context.GatewayPrincipal;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.component.gateway.core.security.GatewayIdentityMapper;
import top.egon.cola.component.gateway.core.security.TrustedIdentity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Maps only immutable identity and version claims; authorization facts never enter headers.
 */
public final class Rbac3TrustedIdentityMapper implements GatewayIdentityMapper {

    public static final String MAPPER_ID = "rbac3-trusted-identity";

    @Override
    public String mapperId() {
        return MAPPER_ID;
    }

    @Override
    public Set<GatewayProtocol> supportedProtocols() {
        return Set.of(GatewayProtocol.HTTP);
    }

    @Override
    public TrustedIdentity map(GatewayAuthContext context) {
        GatewayPrincipal principal = context.principal();
        if (!principal.authenticated() || principal.tenantId() == null) {
            throw new IllegalArgumentException("authenticated RBAC3 principal is required");
        }
        Map<String, String> attributes = principal.attributes();
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("x-egon-gateway-tenant-id", principal.tenantId());
        headers.put("x-egon-gateway-user-id", principal.principalId());
        headers.put("x-egon-gateway-session-id", required(attributes, "rbac3.session-id"));
        headers.put("x-egon-gateway-auth-version", required(attributes, "rbac3.auth-version"));
        headers.put("x-egon-gateway-session-version", required(
                attributes, "rbac3.session-version"));
        headers.put("x-egon-gateway-policy-version", required(
                attributes, "rbac3.policy-version"));
        headers.put("x-egon-gateway-trace-id", context.traceId());
        return new TrustedIdentity(headers, Map.of());
    }

    private String required(Map<String, String> values, String name) {
        String value = values.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing verified claim " + name);
        }
        return value;
    }
}
