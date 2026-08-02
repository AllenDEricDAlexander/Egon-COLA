package top.egon.cola.platform.idp.gateway.security;

import java.util.Locale;
import java.util.Set;

/**
 * Declares every IdP identity header that must be removed before mapping.
 */
public final class IdpReservedHeaderSanitizer {

    private static final Set<String> RESERVED = Set.of(
            "authorization",
            "x-egon-identity-sub",
            "x-egon-tenant-id",
            "x-egon-session-id",
            "x-egon-client-id",
            "x-egon-token-id"
    );

    public Set<String> fieldsToRemove() {
        return RESERVED;
    }

    public boolean trustedIdentityHeader(String name) {
        return name != null
                && RESERVED.contains(name.toLowerCase(Locale.ROOT))
                && !"authorization".equalsIgnoreCase(name);
    }
}
