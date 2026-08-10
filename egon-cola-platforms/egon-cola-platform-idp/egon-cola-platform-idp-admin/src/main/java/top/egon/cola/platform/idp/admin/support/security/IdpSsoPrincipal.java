package top.egon.cola.platform.idp.admin.support.security;

import java.security.Principal;

/** Authenticated browser SSO identity carrying the stable cross-client session ID. */
public record IdpSsoPrincipal(String identitySub, String sessionId)
        implements Principal {

    public IdpSsoPrincipal {
        identitySub = required(identitySub, "identitySub");
        sessionId = required(sessionId, "sessionId");
    }

    @Override
    public String getName() {
        return identitySub;
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }
}
