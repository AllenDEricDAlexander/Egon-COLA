package top.egon.cola.platform.rbac3.gateway.security;

import java.util.Set;

/**
 * Declares the only RBAC3 identity headers the Gateway may generate.
 */
public final class Rbac3ReservedHeaderSanitizer {

    private static final Set<String> RESERVED = Set.of(
            "authorization",
            "x-egon-gateway-tenant-id",
            "x-egon-gateway-user-id",
            "x-egon-gateway-session-id",
            "x-egon-gateway-auth-version",
            "x-egon-gateway-session-version",
            "x-egon-gateway-policy-version",
            "x-egon-gateway-trace-id"
    );

    public Set<String> fieldsToRemove() {
        return RESERVED;
    }

    public boolean trustedIdentityHeader(String name) {
        return name != null && RESERVED.contains(name.toLowerCase(java.util.Locale.ROOT))
                && !"authorization".equalsIgnoreCase(name);
    }
}
