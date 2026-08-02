package top.egon.cola.platform.idp.gateway.security;

import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.core.context.GatewayPrincipal;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.component.gateway.core.security.GatewayIdentityMapper;
import top.egon.cola.component.gateway.core.security.TrustedIdentity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Maps only immutable verified identity claims into the fixed trusted headers.
 */
public final class IdpTrustedIdentityMapper implements GatewayIdentityMapper {

    public static final String MAPPER_ID = "idp-identity";

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
            throw new IllegalArgumentException(
                    "authenticated IdP principal is required");
        }
        Map<String, String> attributes = principal.attributes();
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Egon-Identity-Sub", principal.principalId());
        headers.put("X-Egon-Tenant-Id", principal.tenantId());
        headers.put("X-Egon-Session-Id", required(
                attributes, "idp.session-id"));
        headers.put("X-Egon-Client-Id", required(
                attributes, "idp.client-id"));
        headers.put("X-Egon-Token-Id", required(
                attributes, "idp.token-id"));
        return new TrustedIdentity(headers, Map.of());
    }

    private String required(Map<String, String> values, String name) {
        String value = values.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "missing verified claim " + name);
        }
        return value;
    }
}
