package top.egon.cola.platform.rbac3.admin.runtime.repository.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.RuntimePublicationVO;
import top.egon.cola.platform.rbac3.admin.activation.repository.RoleActivationRuntimeRepository;
import top.egon.cola.platform.rbac3.admin.authorization.domain.vo.SnapshotRecordVO;
import top.egon.cola.platform.rbac3.admin.authorization.repository.AuthorizationSnapshotRepository;
import top.egon.cola.platform.rbac3.admin.authorization.repository.FenceVerifier;
import top.egon.cola.platform.rbac3.admin.runtime.domain.dto.PublishCommandDTO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.PublishResultVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.RuntimeUserAuthorizationVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.UserSnapshotProjectionVO;
import top.egon.cola.platform.rbac3.admin.runtime.repository.RuntimePublicationRepository;
import top.egon.cola.platform.rbac3.contract.authorization.UserAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import top.egon.cola.platform.rbac3.core.runtime.Rbac3RuntimeKeyFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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

    public RedisAuthorizationRuntimeRepository(
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson,
            ObjectMapper objectMapper,
            Rbac3RuntimeKeyFactory keyFactory) {
        this(redisson, objectMapper, keyFactory, Clock.systemUTC());
    }

    @Autowired
    public RedisAuthorizationRuntimeRepository(
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson,
            ObjectMapper objectMapper,
            Rbac3RuntimeKeyFactory keyFactory,
            Clock clock) {
        this.redisson = Objects.requireNonNull(redisson, "redisson");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.keyFactory = Objects.requireNonNull(keyFactory, "keyFactory");
        this.clock = Objects.requireNonNull(clock, "clock");
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
        if (!command.tenantId().equals(user.tenantId())
                || !command.identitySub().equals(user.identitySub())
                || !command.userId().equals(user.userId())
                || command.authVersion() != user.authVersion()
                || command.policyVersion() != user.policyVersion()
                || !command.identitySub().equals(snapshot.identitySub())
                || !command.userId().equals(snapshot.rbacUserId())) {
            throw new IllegalArgumentException("authorization publication identity mismatch");
        }
        String currentJson = bucket(keyFactory.user(
                command.tenantId(), command.identitySub())).get();
        if (currentJson != null) {
            RuntimeUserAuthorizationVO current = read(currentJson, RuntimeUserAuthorizationVO.class);
            if (current.authVersion() > command.authVersion()) {
                throw new IllegalStateException("RBAC3_RUNTIME_VERSION_CONFLICT");
            }
        }
        Duration ttl = ttl(user.expiresAt());
        bucket(keyFactory.authVersion(command.tenantId(), command.userId()))
                .set(Long.toString(command.authVersion()), ttl);
        bucket(keyFactory.policyVersion(command.tenantId()))
                .set(Long.toString(command.policyVersion()), ttl);
        bucket(keyFactory.snapshot(command.tenantId(), command.identitySub(),
                command.authVersion())).set(json(snapshot), ttl);
        bucket(keyFactory.user(command.tenantId(), command.identitySub()))
                .set(json(user), ttl);
        bucket(keyFactory.authorizationPublicationGuard(
                command.tenantId(), command.identitySub())).delete();
        return new PublishResultVO(currentJson == null, snapshot.checksum());
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
