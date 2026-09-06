package top.egon.cola.platform.rbac3.admin.authorization.runtime.repository.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.core.io.ClassPathResource;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.activation.domain.vo.RuntimePublicationVO;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.activation.repository.RoleActivationRuntimeRepository;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.decision.domain.vo.SnapshotRecordVO;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.decision.repository.AuthorizationSnapshotRepository;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.decision.repository.FenceVerifier;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.domain.dto.PublishCommandDTO;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.domain.vo.PublishResultVO;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.domain.vo.RuntimeUserAuthorizationVO;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.domain.vo.UserSnapshotProjectionVO;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.repository.RuntimePublicationRepository;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.repository.InitialAuthorizationContextRepository;
import top.egon.cola.platform.rbac3.contract.authorization.GatewayBizAppScopeSnapshot;
import top.egon.cola.platform.rbac3.contract.authorization.UserAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import top.egon.cola.platform.rbac3.core.runtime.Rbac3RuntimeKeyFactory;
import top.egon.cola.platform.rbac3.starter.cache.AuthorizationSnapshotCache;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;

/**
 * Redis runtime publication keyed by tenant and IdP subject.
 */
@Repository
public class RedisAuthorizationRuntimeRepository implements
        RoleActivationRuntimeRepository,
        AuthorizationSnapshotRepository,
        FenceVerifier,
        RuntimePublicationRepository {

    private final RedissonClient redisson;
    private final ObjectMapper objectMapper;
    private final Rbac3RuntimeKeyFactory keyFactory;
    private final Clock clock;
    private final InitialAuthorizationContextRepository authorizationContext;
    private final AuthorizationSnapshotCache authorizationCache;
    private static final String PUBLISH_SCRIPT = script("redis/rbac3-publish-authorization.lua");
    private static final String INVALIDATE_SCRIPT = script("redis/rbac3-invalidate-authorization.lua");

    @Autowired
    public RedisAuthorizationRuntimeRepository(
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson,
            ObjectMapper objectMapper,
            Rbac3RuntimeKeyFactory keyFactory,
            Clock clock,
            InitialAuthorizationContextRepository authorizationContext,
            AuthorizationSnapshotCache authorizationCache) {
        this.redisson = Objects.requireNonNull(redisson, "redisson");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.keyFactory = Objects.requireNonNull(keyFactory, "keyFactory");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.authorizationContext = Objects.requireNonNull(authorizationContext, "authorizationContext");
        this.authorizationCache = Objects.requireNonNull(authorizationCache, "authorizationCache");
    }

    @Override
    public void publish(RuntimePublicationVO publication) {
        publish(new PublishCommandDTO(
                publication.tenantId(), publication.identitySub(), publication.userId(),
                publication.authVersion(), publication.policyVersion(),
                publication.projection()));
    }

    @Override
    public PublishResultVO publish(PublishCommandDTO command) {
        Objects.requireNonNull(command, "command");
        UserSnapshotProjectionVO projection = Objects.requireNonNull(
                command.projection(), "projection");
        RuntimeUserAuthorizationVO user = projection.user();
        UserAuthorizationSnapshot snapshot = projection.snapshot();
        GatewayBizAppScopeSnapshot gatewayScope = projection.gatewayScope();
        if (!command.tenantId().equals(user.tenantId())
                || !command.identitySub().equals(user.identitySub())
                || !command.userId().equals(user.userId())
                || command.authVersion() != user.authVersion()
                || command.policyVersion() != user.policyVersion()
                || !command.tenantId().equals(snapshot.tenantId())
                || !command.identitySub().equals(snapshot.identitySub())
                || !command.userId().equals(snapshot.rbacUserId())
                || command.authVersion() != snapshot.authVersion()
                || command.policyVersion() != snapshot.policyVersion()
                || !user.expiresAt().equals(snapshot.expiresAt())
                || !command.tenantId().equals(gatewayScope.tenantId())
                || !command.identitySub().equals(gatewayScope.identitySub())
                || !command.userId().equals(gatewayScope.rbacUserId())
                || command.authVersion() != gatewayScope.authVersion()
                || command.policyVersion() != gatewayScope.policyVersion()
                || !snapshot.generatedAt().equals(gatewayScope.generatedAt())
                || !snapshot.expiresAt().equals(gatewayScope.expiresAt())) {
            throw new IllegalArgumentException("authorization publication identity mismatch");
        }
        requireCurrentAuthorization(user);
        Set<String> affectedSystems = cachedSystems(command.tenantId(), command.identitySub());
        snapshot.appContexts().forEach(context -> affectedSystems.add(context.applicationCode()));
        Long published = redisson.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE, PUBLISH_SCRIPT, RScript.ReturnType.INTEGER,
                List.of(keyFactory.snapshot(command.tenantId(), command.identitySub(), command.authVersion()),
                        keyFactory.gatewayScope(command.tenantId(), command.identitySub(), command.authVersion()),
                        keyFactory.authVersion(command.tenantId(), command.userId()),
                        keyFactory.policyVersion(command.tenantId()),
                        keyFactory.user(command.tenantId(), command.identitySub()),
                        keyFactory.authorizationPublicationGuard(command.tenantId(), command.identitySub())),
                json(snapshot), json(gatewayScope), Long.toString(command.authVersion()),
                Long.toString(command.policyVersion()), json(user), Long.toString(ttl(user.expiresAt()).toMillis()));
        if (published == null || published < 0L) {
            throw new IllegalStateException("RBAC3_RUNTIME_VERSION_CONFLICT");
        }
        affectedSystems.forEach(system -> authorizationCache.invalidateUser(
                system, command.tenantId(), command.identitySub()));
        return new PublishResultVO(published == 1L, snapshot.checksum());
    }

    @Override
    public void createFence(
            String tenantId,
            String identitySub,
            String mutationId,
            Duration ttl) {
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("fence ttl must be positive");
        }
        bucket(keyFactory.authorizationPublicationGuard(tenantId, identitySub))
                .set(mutationId, ttl);
    }

    @Override
    public boolean isFenced(String tenantId, String identitySub) {
        return bucket(keyFactory.authorizationPublicationGuard(tenantId, identitySub))
                .isExists();
    }

    /**
     * Removes a user publication guard after the new snapshot is visible.
     */
    public void removeFence(String tenantId, String identitySub) {
        bucket(keyFactory.authorizationPublicationGuard(tenantId, identitySub)).delete();
    }

    /** Removes an unusable publication while retaining monotonic version watermarks. */
    @Override
    public void invalidate(String tenantId, String identitySub, String userId,
                           long authVersion, long policyVersion) {
        if (authVersion < 0L || policyVersion < 0L) {
            throw new IllegalArgumentException("authorization versions must not be negative");
        }
        Set<String> affectedSystems = cachedSystems(tenantId, identitySub);
        Long invalidated = redisson.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE, INVALIDATE_SCRIPT, RScript.ReturnType.INTEGER,
                List.of(keyFactory.authVersion(tenantId, userId), keyFactory.policyVersion(tenantId),
                        keyFactory.user(tenantId, identitySub)),
                Long.toString(authVersion), Long.toString(policyVersion));
        if (invalidated == null || invalidated < 0L) {
            throw new IllegalStateException("RBAC3_RUNTIME_VERSION_CONFLICT");
        }
        affectedSystems.forEach(system -> authorizationCache.invalidateUser(system, tenantId, identitySub));
    }

    /** Includes removed application contexts and the pre-activation self context. */
    private Set<String> cachedSystems(String tenantId, String identitySub) {
        Set<String> systems = new HashSet<>(Set.of("rbac3-admin"));
        String userJson = bucket(keyFactory.user(tenantId, identitySub)).get();
        if (userJson == null) {
            return systems;
        }
        RuntimeUserAuthorizationVO previous = read(userJson, RuntimeUserAuthorizationVO.class);
        String snapshotJson = bucket(keyFactory.snapshot(tenantId, identitySub, previous.authVersion())).get();
        if (snapshotJson != null) {
            UserAuthorizationSnapshot previousSnapshot = read(snapshotJson, UserAuthorizationSnapshot.class);
            if (!tenantId.equals(previousSnapshot.tenantId()) || !identitySub.equals(previousSnapshot.identitySub())) {
                throw new IllegalStateException("authorization cache identity mismatch");
            }
            previousSnapshot.appContexts().forEach(context -> systems.add(context.applicationCode()));
        }
        return systems;
    }

    @Override
    public SnapshotRecordVO load(String tenantId, String identitySub) {
        try {
            RuntimeUserAuthorizationVO user = read(
                    required(bucket(keyFactory.user(tenantId, identitySub)).get(),
                            "AUTH_SNAPSHOT_NOT_READY"),
                    RuntimeUserAuthorizationVO.class);
            Instant now = clock.instant();
            if (!tenantId.equals(user.tenantId())
                    || !identitySub.equals(user.identitySub())
                    || !"ACTIVE".equals(user.status())
                    || !user.expiresAt().isAfter(now)) {
                throw new Rbac3RuleViolation("IDENTITY_INACTIVE");
            }
            if (isFenced(tenantId, identitySub)) {
                throw new Rbac3RuleViolation("AUTH_PROPAGATION_PENDING");
            }
            long authVersion = version(bucket(keyFactory.authVersion(
                    tenantId, user.userId())).get());
            long policyVersion = version(bucket(keyFactory.policyVersion(tenantId)).get());
            if (authVersion != user.authVersion()) {
                throw new Rbac3RuleViolation("AUTH_VERSION_MISMATCH");
            }
            if (policyVersion != user.policyVersion()) {
                throw new Rbac3RuleViolation("POLICY_VERSION_MISMATCH");
            }
            UserAuthorizationSnapshot snapshot = read(
                    required(bucket(keyFactory.snapshot(
                                    tenantId, identitySub, user.authVersion())).get(),
                            "AUTH_SNAPSHOT_NOT_READY"),
                    UserAuthorizationSnapshot.class);
            if (!tenantId.equals(snapshot.tenantId())
                    || !identitySub.equals(snapshot.identitySub())
                    || !user.userId().equals(snapshot.rbacUserId())
                    || snapshot.authVersion() != user.authVersion()
                    || snapshot.policyVersion() != user.policyVersion()
                    || !snapshot.expiresAt().isAfter(now)) {
                throw new Rbac3RuleViolation("AUTH_VERSION_MISMATCH");
            }
            // The outbox may still be rebuilding after a committed revocation. Never
            // authorize a Redis snapshot whose PostgreSQL versions are already stale.
            requireCurrentAuthorization(user);
            return new SnapshotRecordVO(tenantId, identitySub, user.userId(), snapshot);
        } catch (Rbac3RuleViolation exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new Rbac3RuleViolation("AUTH_RUNTIME_UNAVAILABLE");
        }
    }

    private RBucket<String> bucket(String key) {
        return redisson.getBucket(key, StringCodec.INSTANCE);
    }

    private void requireCurrentAuthorization(RuntimeUserAuthorizationVO user) {
        var current = authorizationContext.find(user.tenantId(), user.identitySub())
                .orElseThrow(() -> new Rbac3RuleViolation("IDENTITY_INACTIVE"));
        if (!current.userId().equals(user.userId()) || current.authVersion() != user.authVersion()) {
            throw new Rbac3RuleViolation("AUTH_VERSION_MISMATCH");
        }
        if (current.policyVersion() != user.policyVersion()) {
            throw new Rbac3RuleViolation("POLICY_VERSION_MISMATCH");
        }
    }

    private static String script(String path) {
        try {
            return new ClassPathResource(path)
                    .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("cannot load RBAC3 runtime publication script", exception);
        }
    }

    private Duration ttl(Instant expiresAt) {
        Duration value = Duration.between(clock.instant(), expiresAt);
        return !value.isNegative() && !value.isZero() ? value : Duration.ofSeconds(1);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("cannot encode RBAC3 runtime projection", exception);
        }
    }

    private <T> T read(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("cannot decode RBAC3 runtime projection", exception);
        }
    }

    private String required(String value, String reason) {
        if (value == null) {
            throw new Rbac3RuleViolation(reason);
        }
        return value;
    }

    private long version(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            return Long.parseLong(text);
        }
        throw new IllegalStateException("runtime version is missing");
    }
}
