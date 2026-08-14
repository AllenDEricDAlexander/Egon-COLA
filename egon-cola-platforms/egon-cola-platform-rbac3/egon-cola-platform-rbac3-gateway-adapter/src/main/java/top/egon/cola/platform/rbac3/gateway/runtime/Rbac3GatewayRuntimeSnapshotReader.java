package top.egon.cola.platform.rbac3.gateway.runtime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.core.context.GatewayPrincipal;
import top.egon.cola.component.gateway.core.security.AuthorizationDecision;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.platform.rbac3.contract.authorization.AppAuthorizationContext;
import top.egon.cola.platform.rbac3.contract.authorization.UserAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.core.runtime.Rbac3RuntimeKeyFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Reads the published RBAC3 authorization projection for an already
 * authenticated IdP USER principal and evaluates the operation mapping.
 *
 * <p>This class is intentionally not an authentication component. It does not
 * parse bearer credentials, verify JWTs, refresh tokens, or maintain sessions.
 * The IdP Gateway adapter owns those concerns and supplies the principal.</p>
 */
public final class Rbac3GatewayRuntimeSnapshotReader {

    private final RedissonClient redisson;
    private final ObjectMapper objectMapper;
    private final Rbac3RuntimeKeyFactory keyFactory;
    private final Clock clock;

    public Rbac3GatewayRuntimeSnapshotReader(
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson,
            ObjectMapper objectMapper,
            Rbac3RuntimeKeyFactory keyFactory,
            Clock clock
    ) {
        this.redisson = Objects.requireNonNull(redisson, "redisson");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.keyFactory = Objects.requireNonNull(keyFactory, "keyFactory");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Evaluates the RBAC3 permission for one gateway request.
     */
    public AuthorizationDecision authorize(GatewayAuthContext context) {
        if (!context.principal().authenticated()) {
            return AuthorizationDecision.deny("RBAC3_PRINCIPAL_REQUIRED");
        }
        GatewayPrincipal principal = context.principal();
        if (!"USER".equalsIgnoreCase(principal.principalType())
                || principal.tenantId() == null
                || principal.tenantId().isBlank()
                || principal.principalId().isBlank()) {
            return AuthorizationDecision.deny("RBAC3_USER_PRINCIPAL_REQUIRED");
        }

        Map<String, String> route = context.attributes();
        String definitionSetId = route.get("rbac3.definition-set-id");
        Long mappingVersion = number(route.get("rbac3.mapping-version"));
        if (definitionSetId == null || mappingVersion == null) {
            return AuthorizationDecision.deny("RBAC3_OPERATION_MAPPING_MISSING");
        }

        UserAuthorizationSnapshot snapshot = runtime(
                principal.tenantId(), principal.principalId());
        List<OperationPermissionMapping> mappings = mappings(redisson.getBucket(
                keyFactory.operationMapping(
                        principal.tenantId(), definitionSetId,
                        context.operationId(), mappingVersion)).get());
        List<OperationPermissionMapping> active = mappings.stream()
                .filter(OperationPermissionMapping::active)
                .filter(mapping -> principal.tenantId().equals(mapping.tenantId()))
                .filter(mapping -> definitionSetId.equals(mapping.definitionSetId()))
                .filter(mapping -> context.operationId().equals(
                        mapping.gatewayOperationId()))
                .filter(mapping -> mappingVersion == mapping.mappingVersion())
                .toList();
        if (active.size() != 1) {
            return AuthorizationDecision.deny(active.isEmpty()
                    ? "RBAC3_OPERATION_MAPPING_MISSING"
                    : "RBAC3_OPERATION_MAPPING_CONFLICT");
        }

        OperationPermissionMapping mapping = active.getFirst();
        if (!context.policyId().equals(mapping.securityPolicyId())) {
            return AuthorizationDecision.deny("RBAC3_SECURITY_POLICY_MISMATCH");
        }
        if (context.accessZone() == AccessZone.PUBLIC
                && !mapping.externalAccessible()) {
            return AuthorizationDecision.deny("RBAC3_OPERATION_NOT_EXTERNAL");
        }
        List<AppAuthorizationContext> applications = snapshot.appContexts().stream()
                .filter(application -> mapping.applicationCode().equals(
                        application.applicationCode()))
                .toList();
        if (applications.size() != 1) {
            return AuthorizationDecision.deny("RBAC3_APPLICATION_CONTEXT_INVALID");
        }
        return applications.getFirst().permissions().contains(mapping.permissionCode())
                ? AuthorizationDecision.allow()
                : AuthorizationDecision.deny("RBAC3_PERMISSION_DENIED");
    }

    /**
     * Loads one version-consistent user authorization projection. A missing,
     * fenced, stale, malformed, or unavailable projection fails closed as a
     * runtime-unavailable condition; the provider maps that condition to 503.
     */
    private UserAuthorizationSnapshot runtime(
            String tenantId,
            String identitySub
    ) {
        try {
            RuntimeUserAuthorization user = convert(redisson.getBucket(
                            keyFactory.user(tenantId, identitySub)).get(),
                    RuntimeUserAuthorization.class);
            Instant now = clock.instant();
            if (!tenantId.equals(user.tenantId())
                    || !identitySub.equals(user.identitySub())
                    || !"ACTIVE".equals(user.status())
                    || !user.expiresAt().isAfter(now)) {
                throw unavailable("RBAC3_USER_AUTHORIZATION_INVALID");
            }
            if (redisson.getBucket(keyFactory.authorizationPublicationGuard(
                    tenantId, identitySub)).isExists()) {
                throw unavailable("RBAC3_AUTHORIZATION_PUBLICATION_PENDING");
            }
            long authVersion = version(redisson.getBucket(
                    keyFactory.authVersion(tenantId, user.userId())).get());
            long policyVersion = version(redisson.getBucket(
                    keyFactory.policyVersion(tenantId)).get());
            if (authVersion != user.authVersion()
                    || policyVersion != user.policyVersion()) {
                throw unavailable("RBAC3_RUNTIME_VERSION_MISMATCH");
            }

            UserAuthorizationSnapshot snapshot = convert(redisson.getBucket(
                            keyFactory.snapshot(tenantId, identitySub, user.authVersion())).get(),
                    UserAuthorizationSnapshot.class);
            if (!tenantId.equals(snapshot.tenantId())
                    || !identitySub.equals(snapshot.identitySub())
                    || !user.userId().equals(snapshot.rbacUserId())
                    || snapshot.authVersion() != user.authVersion()
                    || snapshot.policyVersion() != user.policyVersion()
                    || !snapshot.expiresAt().isAfter(now)) {
                throw unavailable("RBAC3_SNAPSHOT_VERSION_MISMATCH");
            }
            return snapshot;
        } catch (RuntimeUnavailableException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new RuntimeUnavailableException(
                    "RBAC3_AUTHORIZATION_RUNTIME_UNAVAILABLE", exception);
        }
    }

    private List<OperationPermissionMapping> mappings(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Map<?, ?> map && map.get("mappings") != null) {
            return objectMapper.convertValue(
                    map.get("mappings"), new TypeReference<>() { });
        }
        if (value instanceof Collection<?>) {
            return objectMapper.convertValue(value, new TypeReference<>() { });
        }
        return List.of(convert(value, OperationPermissionMapping.class));
    }

    private <T> T convert(Object value, Class<T> type) {
        if (value == null) {
            throw new IllegalArgumentException("runtime value is missing");
        }
        return objectMapper.convertValue(value, type);
    }

    private long version(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            return Long.parseLong(text);
        }
        if (value instanceof Map<?, ?> map && map.get("value") != null) {
            return version(map.get("value"));
        }
        throw new IllegalArgumentException("runtime version is missing");
    }

    private Long number(String value) {
        try {
            long number = Long.parseLong(value);
            return number < 0 ? null : number;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private RuntimeUnavailableException unavailable(String code) {
        return new RuntimeUnavailableException(code, null);
    }

    /**
     * JSON shape of the Redis user publication pointer.
     */
    public record RuntimeUserAuthorization(
            String tenantId,
            String identitySub,
            String userId,
            String status,
            long authVersion,
            long policyVersion,
            Instant expiresAt
    ) {
    }

    /** JSON shape of a published operation-to-permission mapping. */
    public record OperationPermissionMapping(
            String tenantId,
            String applicationCode,
            String definitionSetId,
            String gatewayOperationId,
            long mappingVersion,
            String permissionCode,
            boolean externalAccessible,
            String securityPolicyId,
            boolean active
    ) {
    }

    public static final class RuntimeUnavailableException extends RuntimeException {

        public RuntimeUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
