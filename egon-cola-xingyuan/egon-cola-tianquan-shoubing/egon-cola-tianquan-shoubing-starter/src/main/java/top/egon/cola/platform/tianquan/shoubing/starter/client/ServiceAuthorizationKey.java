package top.egon.cola.platform.tianquan.shoubing.starter.client;

import top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Complete non-secret identity of one SERVICE-token authorization cache entry.
 *
 * @param registrationId Spring registration id
 * @param appId business application id
 * @param audience exact Resource URI
 * @param context TENANT or PLATFORM
 * @param tenantId tenant dimension, absent for PLATFORM
 * @param scopes normalized scope set
 */
public record ServiceAuthorizationKey(
        String issuer,
        String registrationId,
        String clientId,
        long credentialVersion,
        String appId,
        URI audience,
        ServiceTokenContext context,
        String tenantId,
        Set<String> scopes
) {

    public ServiceAuthorizationKey {
        issuer = required(issuer, "issuer", 2_048);
        URI issuerUri;
        try {
            issuerUri = URI.create(issuer);
        } catch (IllegalArgumentException malformed) {
            throw new IdpServiceTokenRequest.IdpServiceTokenRequestException(
                    "issuer is invalid");
        }
        if (!issuerUri.isAbsolute() || issuerUri.getHost() == null
                || !issuerUri.toASCIIString().equals(issuer)) {
            throw new IdpServiceTokenRequest.IdpServiceTokenRequestException(
                    "issuer is invalid");
        }
        registrationId = required(registrationId, "registrationId");
        clientId = required(clientId, "clientId");
        if (credentialVersion < 0L) {
            throw new IdpServiceTokenRequest.IdpServiceTokenRequestException(
                    "credentialVersion is invalid");
        }
        appId = required(appId, "appId");
        audience = Objects.requireNonNull(audience, "audience");
        context = Objects.requireNonNull(context, "context");
        tenantId = context == ServiceTokenContext.TENANT
                ? required(tenantId, "tenantId") : null;
        TreeSet<String> normalized = new TreeSet<>(
                Objects.requireNonNull(scopes, "scopes"));
        if (normalized.isEmpty() || normalized.size() > 64) {
            throw new IdpServiceTokenRequest.IdpServiceTokenRequestException(
                    "scopes are invalid");
        }
        scopes = Collections.unmodifiableSet(new LinkedHashSet<>(normalized));
    }

    /** Creates the cache key from the request and its trusted registration identity. */
    public static ServiceAuthorizationKey from(
            IdpServiceTokenRequest request,
            String issuer,
            String clientId,
            long credentialVersion) {
        if (request == null) {
            throw new IdpServiceTokenRequest.IdpServiceTokenRequestException(
                    "request is required"
            );
        }
        return new ServiceAuthorizationKey(
                issuer,
                request.registrationId(),
                clientId,
                credentialVersion,
                request.appId(),
                request.audience(),
                request.context(),
                request.tenantId(),
                request.scopes()
        );
    }

    /** Keeps the old source signature while rejecting cache keys without a trusted identity. */
    @Deprecated
    public static ServiceAuthorizationKey from(IdpServiceTokenRequest request) {
        throw new IdpServiceTokenRequest.IdpServiceTokenRequestException(
                "trusted registration identity is required");
    }

    /** Stable manager principal name that prevents cross-dimension reuse. */
    public String principalName() {
        return lengthPrefix(registrationId)
                + lengthPrefix(issuer)
                + lengthPrefix(clientId)
                + lengthPrefix(Long.toString(credentialVersion))
                + lengthPrefix(appId)
                + lengthPrefix(audience.toString())
                + lengthPrefix(context.name())
                + lengthPrefix(tenantId == null ? "" : tenantId)
                + scopes.stream().map(ServiceAuthorizationKey::lengthPrefix)
                .reduce("", String::concat);
    }

    private static String lengthPrefix(String value) {
        return value.length() + ":" + value;
    }

    private static String required(String value, String field) {
        return required(value, field, 128);
    }

    private static String required(String value, String field, int maximumLength) {
        if (value == null || value.isBlank() || !value.equals(value.trim())
                || value.length() > maximumLength) {
            throw new IdpServiceTokenRequest.IdpServiceTokenRequestException(
                    field + " is invalid");
        }
        return value;
    }
}
