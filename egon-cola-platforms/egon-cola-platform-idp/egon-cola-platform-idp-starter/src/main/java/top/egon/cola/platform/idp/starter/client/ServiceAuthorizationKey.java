package top.egon.cola.platform.idp.starter.client;

import top.egon.cola.platform.idp.contract.ServiceTokenContext;

import java.net.URI;
import java.util.Set;

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
        String registrationId,
        String appId,
        URI audience,
        ServiceTokenContext context,
        String tenantId,
        Set<String> scopes
) {

    /** Creates a cache key from the validated request. */
    public static ServiceAuthorizationKey from(IdpServiceTokenRequest request) {
        if (request == null) {
            throw new IdpServiceTokenRequest.IdpServiceTokenRequestException(
                    "request is required"
            );
        }
        return new ServiceAuthorizationKey(
                request.registrationId(),
                request.appId(),
                request.audience(),
                request.context(),
                request.tenantId(),
                request.scopes()
        );
    }

    /** Stable manager principal name that prevents cross-dimension reuse. */
    public String principalName() {
        return lengthPrefix(registrationId)
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
}
