package top.egon.cola.platform.idp.admin.token.service.impl;

import top.egon.cola.platform.idp.admin.token.service.SigningKeyRuntime;

import java.util.Objects;

/**
 * Restricts runtime activation to the externally mounted PEM key.
 */
public final class ExternalPemSigningKeyRuntime implements SigningKeyRuntime {

    private final String configuredKid;

    public ExternalPemSigningKeyRuntime(String configuredKid) {
        if (configuredKid == null
                || configuredKid.isBlank()
                || !configuredKid.equals(configuredKid.trim())) {
            throw new IllegalArgumentException("configured kid is required");
        }
        this.configuredKid = configuredKid;
    }

    @Override
    public void activate(String kid) {
        if (!configuredKid.equals(Objects.requireNonNull(kid, "kid"))) {
            throw new IllegalStateException(
                    "signing key is not mounted in the OAuth runtime"
            );
        }
    }

    @Override
    public boolean isServing(String kid) {
        return configuredKid.equals(kid);
    }
}
