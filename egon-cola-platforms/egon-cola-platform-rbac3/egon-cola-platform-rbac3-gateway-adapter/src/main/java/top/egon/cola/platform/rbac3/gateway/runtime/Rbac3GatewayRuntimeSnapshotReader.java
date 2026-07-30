package top.egon.cola.platform.rbac3.gateway.runtime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.core.context.GatewayPrincipal;
import top.egon.cola.component.gateway.core.security.AuthorizationDecision;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.platform.rbac3.contract.auth.Rbac3TokenClaims;
import top.egon.cola.platform.rbac3.contract.authorization.AppAuthorizationContext;
import top.egon.cola.platform.rbac3.contract.authorization.SessionAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.core.runtime.Rbac3RuntimeKeyFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Reads only Redis runtime projections and requires one version-consistent fact set.
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

    public void verifySession(Rbac3TokenClaims claims) {
        runtime(claims.tid(), claims.sub(), claims.sid(),
                claims.av(), claims.sv(), claims.pv());
    }

    public AuthorizationDecision authorize(GatewayAuthContext context) {
        if (!context.principal().authenticated()) {
            return AuthorizationDecision.deny("RBAC3_PRINCIPAL_REQUIRED");
        }
        GatewayPrincipal principal = context.principal();
        Map<String, String> claims = principal.attributes();
        String sessionId = claims.get("rbac3.session-id");
        Long authVersion = number(claims.get("rbac3.auth-version"));
        Long sessionVersion = number(claims.get("rbac3.session-version"));
        Long policyVersion = number(claims.get("rbac3.policy-version"));
        if (principal.tenantId() == null || sessionId == null
                || authVersion == null || sessionVersion == null
                || policyVersion == null) {
            return AuthorizationDecision.deny("RBAC3_PRINCIPAL_CLAIMS_INVALID");
        }
        Map<String, String> route = context.attributes();
        String definitionSetId = route.get("rbac3.definition-set-id");
        Long mappingVersion = number(route.get("rbac3.mapping-version"));
        if (definitionSetId == null || mappingVersion == null) {
            return AuthorizationDecision.deny("RBAC3_OPERATION_MAPPING_MISSING");
        }
        SessionAuthorizationSnapshot snapshot = runtime(
                principal.tenantId(), principal.principalId(), sessionId,
                authVersion, sessionVersion, policyVersion);
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

    private SessionAuthorizationSnapshot runtime(
            String tenantId,
            String userId,
            String sessionId,
            long authVersion,
            long sessionVersion,
            long policyVersion
    ) {
        try {
            RuntimeSession session = convert(redisson.getBucket(
                    keyFactory.session(tenantId, sessionId)).get(), RuntimeSession.class);
            if (!tenantId.equals(session.tenantId())
                    || !userId.equals(session.userId())
                    || !sessionId.equals(session.sessionId())
                    || !"ACTIVE".equals(session.status())
                    || !session.expiresAt().isAfter(clock.instant())
                    || authVersion != session.authVersion()
                    || sessionVersion != session.sessionVersion()
                    || policyVersion != session.policyVersion()) {
                throw unavailable("RBAC3_SESSION_INVALID");
            }
            if (version(redisson.getBucket(
                    keyFactory.authVersion(tenantId, userId)).get()) != authVersion
                    || version(redisson.getBucket(
                    keyFactory.policyVersion(tenantId)).get()) != policyVersion) {
                throw unavailable("RBAC3_RUNTIME_VERSION_MISMATCH");
            }
            if (redisson.getBucket(keyFactory.sessionFence(
                    tenantId, sessionId)).isExists()) {
                throw unavailable("RBAC3_SESSION_FENCED");
            }
            SessionAuthorizationSnapshot snapshot = convert(redisson.getBucket(
                    keyFactory.snapshot(tenantId, sessionId, sessionVersion)).get(),
                    SessionAuthorizationSnapshot.class);
            if (!sessionId.equals(snapshot.sessionId())
                    || snapshot.authVersion() != authVersion
                    || snapshot.sessionVersion() != sessionVersion
                    || snapshot.policyVersion() != policyVersion) {
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

    public record RuntimeSession(
            String tenantId,
            String userId,
            String sessionId,
            String status,
            long authVersion,
            long sessionVersion,
            long policyVersion,
            Instant expiresAt
    ) {
    }

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
