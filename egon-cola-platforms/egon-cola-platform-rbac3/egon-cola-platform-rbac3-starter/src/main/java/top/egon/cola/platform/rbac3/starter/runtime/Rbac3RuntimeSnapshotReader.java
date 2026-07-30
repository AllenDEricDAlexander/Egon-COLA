package top.egon.cola.platform.rbac3.starter.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.platform.rbac3.contract.auth.Rbac3TokenClaims;
import top.egon.cola.platform.rbac3.contract.authorization.SessionAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.core.runtime.Rbac3RuntimeKeyFactory;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationService;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Reads one version-consistent authorization snapshot from the dedicated Redis client.
 */
public final class Rbac3RuntimeSnapshotReader {

    private final RedissonClient redisson;
    private final ObjectMapper objectMapper;
    private final Rbac3RuntimeKeyFactory keyFactory;
    private final Clock clock;

    public Rbac3RuntimeSnapshotReader(
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

    public AuthorizationService.RuntimeAuthorizationContext read(
            Rbac3TokenClaims claims
    ) {
        try {
            Object sessionValue = redisson.getBucket(
                    keyFactory.session(claims.tid(), claims.sid())).get();
            RuntimeSession session = convert(sessionValue, RuntimeSession.class);
            validateSession(claims, session);

            long authVersion = version(redisson.getBucket(
                    keyFactory.authVersion(claims.tid(), claims.sub())).get());
            long policyVersion = version(redisson.getBucket(
                    keyFactory.policyVersion(claims.tid())).get());
            if (authVersion != claims.av() || policyVersion != claims.pv()) {
                throw unavailable("AUTHORIZATION_VERSION_MISMATCH", claims);
            }

            Object snapshotValue = redisson.getBucket(keyFactory.snapshot(
                    claims.tid(), claims.sid(), claims.sv())).get();
            SessionAuthorizationSnapshot snapshot = convert(
                    snapshotValue, SessionAuthorizationSnapshot.class);
            boolean fenced = redisson.getBucket(keyFactory.sessionFence(
                    claims.tid(), claims.sid())).isExists();
            return new AuthorizationService.RuntimeAuthorizationContext(
                    claims, snapshot, fenced);
        } catch (AuthorizationService.RuntimeUnavailableException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AuthorizationService.RuntimeUnavailableException(
                    "AUTHORIZATION_RUNTIME_UNAVAILABLE", claims);
        }
    }

    private void validateSession(Rbac3TokenClaims claims, RuntimeSession session) {
        if (session == null
                || !claims.tid().equals(session.tenantId())
                || !claims.sub().equals(session.userId())
                || !claims.sid().equals(session.sessionId())
                || !"ACTIVE".equals(session.status())
                || !session.expiresAt().isAfter(clock.instant())
                || claims.av() != session.authVersion()
                || claims.sv() != session.sessionVersion()
                || claims.pv() != session.policyVersion()) {
            throw unavailable("AUTHORIZATION_SESSION_INVALID", claims);
        }
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

    private <T> T convert(Object value, Class<T> type) {
        if (value == null) {
            throw new IllegalArgumentException("runtime value is missing");
        }
        return objectMapper.convertValue(value, type);
    }

    private AuthorizationService.RuntimeUnavailableException unavailable(
            String reasonCode,
            Rbac3TokenClaims claims
    ) {
        return new AuthorizationService.RuntimeUnavailableException(reasonCode, claims);
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
}
