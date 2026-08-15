package top.egon.cola.platform.rbac3.gateway.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.gateway.core.context.GatewayPrincipal;
import top.egon.cola.component.gateway.core.security.AuthorizationDecision;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.platform.rbac3.contract.authorization.ApplicationAccessScope;
import top.egon.cola.platform.rbac3.contract.authorization.BusinessAccessScope;
import top.egon.cola.platform.rbac3.contract.authorization.GatewayBizAppScopeSnapshot;
import top.egon.cola.platform.rbac3.core.runtime.Rbac3RuntimeKeyFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Reads the version-consistent Gateway BIZ/APP scope projection for an
 * authenticated IdP USER and authorizes the route target from BIZ to APP.
 *
 * <p>This reader never loads operation mappings or permission data. The
 * downstream application remains responsible for operation authorization.</p>
 */
public final class Rbac3GatewayScopeSnapshotReader {

    private final RedissonClient redisson;
    private final ObjectMapper objectMapper;
    private final Rbac3RuntimeKeyFactory keyFactory;
    private final Clock clock;

    public Rbac3GatewayScopeSnapshotReader(
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
     * Authorizes the server-selected route BIZ before its nested APP.
     */
    public AuthorizationDecision authorize(GatewayAuthContext context) {
        Objects.requireNonNull(context, "context");
        GatewayPrincipal principal = context.principal();
        if (!principal.authenticated()) {
            return AuthorizationDecision.deny("RBAC3_PRINCIPAL_REQUIRED");
        }
        if (!"USER".equalsIgnoreCase(principal.principalType())
                || principal.tenantId() == null
                || principal.tenantId().isBlank()
                || principal.principalId().isBlank()) {
            return AuthorizationDecision.deny("RBAC3_USER_PRINCIPAL_REQUIRED");
        }

        String businessCode = attribute(context, "idp.biz-code");
        if (businessCode == null) {
            return AuthorizationDecision.deny("RBAC3_BUSINESS_SCOPE_REQUIRED");
        }
        String applicationCode = attribute(context, "idp.app-code");
        if (applicationCode == null) {
            return AuthorizationDecision.deny("RBAC3_APPLICATION_SCOPE_REQUIRED");
        }

        GatewayBizAppScopeSnapshot scope = runtime(
                principal.tenantId(), principal.principalId());
        List<BusinessAccessScope> businesses = scope.businesses().stream()
                .filter(business -> businessCode.equals(business.businessCode()))
                .toList();
        if (businesses.isEmpty()) {
            return AuthorizationDecision.deny("RBAC3_BUSINESS_SCOPE_DENIED");
        }
        if (businesses.size() != 1) {
            throw unavailable("RBAC3_BUSINESS_SCOPE_CONFLICT");
        }

        List<ApplicationAccessScope> applications = businesses.getFirst()
                .applications().stream()
                .filter(application -> applicationCode.equals(
                        application.applicationCode()))
                .toList();
        if (applications.isEmpty()) {
            return AuthorizationDecision.deny("RBAC3_APPLICATION_SCOPE_DENIED");
        }
        if (applications.size() != 1) {
            throw unavailable("RBAC3_APPLICATION_SCOPE_CONFLICT");
        }
        return AuthorizationDecision.allow();
    }

    private GatewayBizAppScopeSnapshot runtime(
            String tenantId,
            String identitySub
    ) {
        try {
            RuntimeUserAuthorization user = read(
                    value(keyFactory.user(tenantId, identitySub)),
                    RuntimeUserAuthorization.class);
            Instant now = clock.instant();
            if (!tenantId.equals(user.tenantId())
                    || !identitySub.equals(user.identitySub())
                    || user.userId() == null
                    || user.userId().isBlank()
                    || !"ACTIVE".equals(user.status())
                    || user.expiresAt() == null
                    || !user.expiresAt().isAfter(now)) {
                throw unavailable("RBAC3_USER_AUTHORIZATION_INVALID");
            }
            if (bucket(keyFactory.authorizationPublicationGuard(
                    tenantId, identitySub)).isExists()) {
                throw unavailable("RBAC3_AUTHORIZATION_PUBLICATION_PENDING");
            }
            long authVersion = version(value(keyFactory.authVersion(
                    tenantId, user.userId())));
            long policyVersion = version(value(keyFactory.policyVersion(tenantId)));
            if (authVersion != user.authVersion()
                    || policyVersion != user.policyVersion()) {
                throw unavailable("RBAC3_RUNTIME_VERSION_MISMATCH");
            }

            GatewayBizAppScopeSnapshot scope = read(value(keyFactory.gatewayScope(
                            tenantId, identitySub, user.authVersion())),
                    GatewayBizAppScopeSnapshot.class);
            if (!tenantId.equals(scope.tenantId())
                    || !identitySub.equals(scope.identitySub())
                    || !user.userId().equals(scope.rbacUserId())
                    || scope.authVersion() != user.authVersion()
                    || scope.policyVersion() != user.policyVersion()
                    || !user.expiresAt().equals(scope.expiresAt())
                    || !scope.expiresAt().isAfter(now)) {
                throw unavailable("RBAC3_SCOPE_VERSION_MISMATCH");
            }
            return scope;
        } catch (RuntimeUnavailableException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new RuntimeUnavailableException(
                    "RBAC3_SCOPE_RUNTIME_UNAVAILABLE", exception);
        }
    }

    private String attribute(GatewayAuthContext context, String name) {
        String value = context.attributes().get(name);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private org.redisson.api.RBucket<String> bucket(String key) {
        return redisson.getBucket(key, StringCodec.INSTANCE);
    }

    private String value(String key) {
        return bucket(key).get();
    }

    private <T> T read(String value, Class<T> type) {
        if (value == null) {
            throw new IllegalArgumentException("runtime value is missing");
        }
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("runtime value is malformed", exception);
        }
    }

    private long version(String value) {
        if (value == null) {
            throw new IllegalArgumentException("runtime version is missing");
        }
        return Long.parseLong(value);
    }

    private RuntimeUnavailableException unavailable(String code) {
        return new RuntimeUnavailableException(code, null);
    }

    /** JSON shape of the Redis user authorization pointer. */
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

    public static final class RuntimeUnavailableException extends RuntimeException {

        public RuntimeUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
