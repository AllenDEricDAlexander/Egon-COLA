package top.egon.cola.platform.idp.starter.client;

import top.egon.cola.platform.idp.contract.ServiceTokenContext;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Typed input for one IdP SERVICE-token authorization.
 *
 * <p>The caller supplies only the target resource, tenant context and scopes;
 * client registration and Secret material remain in Spring configuration.</p>
 *
 * @param registrationId Spring OAuth2 Client registration id
 * @param appId stable business application identity
 * @param audience exact target Resource URI
 * @param context server-authorized token context
 * @param tenantId concrete tenant for TENANT context, otherwise {@code null}
 * @param scopes normalized least-privilege scopes
 */
public record IdpServiceTokenRequest(
        String registrationId,
        String appId,
        URI audience,
        ServiceTokenContext context,
        String tenantId,
        Set<String> scopes
) {

    private static final int MAX_TEXT_LENGTH = 128;
    private static final int MAX_SCOPE_COUNT = 64;

    /** Validates and normalizes all caller-controlled authorization dimensions. */
    public IdpServiceTokenRequest {
        registrationId = required(registrationId, "registrationId");
        appId = required(appId, "appId");
        audience = normalizedAudience(audience);
        context = Objects.requireNonNull(context, "context");
        tenantId = context == ServiceTokenContext.TENANT
                ? required(tenantId, "tenantId")
                : null;
        scopes = normalizedScopes(scopes);
    }

    private static String required(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank() || !value.equals(value.trim())
                || value.length() > MAX_TEXT_LENGTH
                || value.indexOf('\u0000') >= 0) {
            throw new IdpServiceTokenRequestException(
                    field + " is invalid"
            );
        }
        return value;
    }

    private static URI normalizedAudience(URI value) {
        Objects.requireNonNull(value, "audience");
        URI normalized = value.normalize();
        if (!normalized.isAbsolute()
                || normalized.getFragment() != null
                || !normalized.equals(value)
                || normalized.toString().length() > MAX_TEXT_LENGTH * 4) {
            throw new IdpServiceTokenRequestException(
                    "audience is invalid"
            );
        }
        return normalized;
    }

    private static Set<String> normalizedScopes(Set<String> values) {
        Objects.requireNonNull(values, "scopes");
        if (values.isEmpty() || values.size() > MAX_SCOPE_COUNT) {
            throw new IdpServiceTokenRequestException(
                    "scopes are invalid"
            );
        }
        TreeSet<String> sorted = new TreeSet<>();
        for (String value : values) {
            String scope = required(value, "scope");
            if (scope.length() > MAX_TEXT_LENGTH || scope.indexOf(' ') >= 0) {
                throw new IdpServiceTokenRequestException(
                        "scope is invalid"
                );
            }
            sorted.add(scope);
        }
        if (sorted.size() != values.size()) {
            throw new IdpServiceTokenRequestException(
                    "scopes contain duplicates"
            );
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(sorted));
    }

    /** Stable caller-facing validation category without credential material. */
    public static class IdpServiceTokenRequestException
            extends IllegalArgumentException {

        /** Creates a validation exception with a safe message. */
        public IdpServiceTokenRequestException(String message) {
            super(message);
        }
    }
}
