package top.egon.cola.platform.rbac3.admin.snapshot.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;
import top.egon.cola.platform.rbac3.admin.activation.application.RoleActivationFacade;
import top.egon.cola.platform.rbac3.admin.authorization.application.AuthorizationDecisionService;
import top.egon.cola.platform.rbac3.admin.snapshot.application.SessionSnapshotProjector;
import top.egon.cola.platform.rbac3.contract.authorization.SessionAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.core.runtime.Rbac3RuntimeKeyFactory;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Publishes the session pointer and its immutable snapshot through one Redis script.
 */
@Repository
public class RedisAuthorizationRuntimeStore implements
        RoleActivationFacade.RuntimeStore,
        AuthorizationDecisionService.SnapshotSource,
        AuthorizationDecisionService.FenceVerifier {

    private static final String PUBLISH_SCRIPT = script(
            "redis/publish-session-snapshot.lua");
    private static final String VERIFY_FENCE_SCRIPT = script(
            "redis/verify-authorization-fence.lua");

    private final RedissonClient redisson;
    private final ObjectMapper objectMapper;
    private final Rbac3RuntimeKeyFactory keyFactory;
    private final Clock clock;

    public RedisAuthorizationRuntimeStore(
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson,
            ObjectMapper objectMapper,
            Rbac3RuntimeKeyFactory keyFactory
    ) {
        this(redisson, objectMapper, keyFactory, Clock.systemUTC());
    }

    @Autowired
    public RedisAuthorizationRuntimeStore(
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

    public PublishResult publish(PublishCommand command) {
        var session = command.projection().session();
        var snapshot = command.projection().snapshot();
        List<Object> keys = List.of(
                keyFactory.session(command.tenantId(), command.sessionId()),
                keyFactory.authVersion(command.tenantId(), command.userId()),
                keyFactory.policyVersion(command.tenantId()),
                keyFactory.snapshot(
                        command.tenantId(), command.sessionId(), command.sessionVersion()),
                keyFactory.sessionFence(command.tenantId(), command.sessionId()));
        Number result = redisson.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                PUBLISH_SCRIPT,
                RScript.ReturnType.INTEGER,
                keys,
                json(session),
                Long.toString(command.authVersion()),
                Long.toString(command.policyVersion()),
                json(snapshot),
                Long.toString(command.sessionVersion()),
                Long.toString(session.expiresAt().toEpochMilli()));
        if (result == null || result.intValue() < 0) {
            throw new IllegalStateException("RBAC3_RUNTIME_VERSION_CONFLICT");
        }
        return new PublishResult(result.intValue() == 1, snapshot.checksum());
    }

    @Override
    public void publish(RoleActivationFacade.RuntimePublication publication) {
        publish(new PublishCommand(
                publication.tenantId(), publication.userId(), publication.sessionId(),
                publication.authVersion(), publication.sessionVersion(),
                publication.policyVersion(), publication.projection()));
    }

    public void createSessionFence(
            String tenantId,
            String sessionId,
            String mutationId,
            Duration ttl
    ) {
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("fence ttl must be positive");
        }
        RBucket<String> bucket = redisson.getBucket(
                keyFactory.sessionFence(tenantId, sessionId), StringCodec.INSTANCE);
        bucket.set(mutationId, ttl);
    }

    @Override
    public void createFence(
            String tenantId,
            String sessionId,
            String mutationId,
            Duration ttl
    ) {
        createSessionFence(tenantId, sessionId, mutationId, ttl);
    }

    public boolean isSessionFenced(String tenantId, String sessionId) {
        Number result = redisson.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_ONLY,
                VERIFY_FENCE_SCRIPT,
                RScript.ReturnType.INTEGER,
                List.of(keyFactory.sessionFence(tenantId, sessionId)));
        return result != null && result.intValue() == 1;
    }

    public void removeSessionFence(String tenantId, String sessionId) {
        redisson.getBucket(
                keyFactory.sessionFence(tenantId, sessionId), StringCodec.INSTANCE)
                .delete();
    }

    @Override
    public AuthorizationDecisionService.SnapshotRecord load(
            String tenantId,
            String sessionId) {
        try {
            RBucket<String> sessionBucket = redisson.getBucket(
                    keyFactory.session(tenantId, sessionId), StringCodec.INSTANCE);
            String sessionJson = sessionBucket.get();
            if (sessionJson == null) {
                throw new Rbac3RuleViolation("AUTH_SNAPSHOT_NOT_READY");
            }
            SessionSnapshotProjector.RuntimeSession session = objectMapper.readValue(
                    sessionJson, SessionSnapshotProjector.RuntimeSession.class);
            if (!tenantId.equals(session.tenantId())
                    || !sessionId.equals(session.sessionId())
                    || !"ACTIVE".equals(session.status())
                    || !session.expiresAt().isAfter(clock.instant())) {
                throw new Rbac3RuleViolation("SESSION_INVALIDATED");
            }
            if (version(redisson.getBucket(
                    keyFactory.authVersion(tenantId, session.userId()),
                    StringCodec.INSTANCE).get()) != session.authVersion()) {
                throw new Rbac3RuleViolation("AUTH_VERSION_MISMATCH");
            }
            if (version(redisson.getBucket(
                    keyFactory.policyVersion(tenantId),
                    StringCodec.INSTANCE).get()) != session.policyVersion()) {
                throw new Rbac3RuleViolation("POLICY_VERSION_MISMATCH");
            }
            if (redisson.getBucket(
                    keyFactory.sessionFence(tenantId, sessionId),
                    StringCodec.INSTANCE).isExists()) {
                throw new Rbac3RuleViolation("AUTH_PROPAGATION_PENDING");
            }
            RBucket<String> snapshotBucket = redisson.getBucket(
                    keyFactory.snapshot(tenantId, sessionId, session.sessionVersion()),
                    StringCodec.INSTANCE);
            String snapshotJson = snapshotBucket.get();
            if (snapshotJson == null) {
                throw new Rbac3RuleViolation("AUTH_SNAPSHOT_NOT_READY");
            }
            SessionAuthorizationSnapshot snapshot = objectMapper.readValue(
                    snapshotJson, SessionAuthorizationSnapshot.class);
            if (!sessionId.equals(snapshot.sessionId())
                    || snapshot.authVersion() != session.authVersion()
                    || snapshot.sessionVersion() != session.sessionVersion()
                    || snapshot.policyVersion() != session.policyVersion()) {
                throw new Rbac3RuleViolation("SESSION_VERSION_MISMATCH");
            }
            return new AuthorizationDecisionService.SnapshotRecord(
                    tenantId, session.identitySub(), session.userId(), snapshot);
        } catch (Rbac3RuleViolation error) {
            throw error;
        } catch (RuntimeException | JsonProcessingException error) {
            throw new Rbac3RuleViolation("AUTH_RUNTIME_UNAVAILABLE");
        }
    }

    @Override
    public boolean isFenced(String tenantId, String sessionId) {
        return isSessionFenced(tenantId, sessionId);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("cannot encode RBAC3 runtime projection", exception);
        }
    }

    private long version(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            return Long.parseLong(text);
        }
        if (value instanceof java.util.Map<?, ?> map && map.get("value") != null) {
            return version(map.get("value"));
        }
        throw new IllegalArgumentException("runtime version is missing");
    }

    private static String script(String location) {
        try (var input = new ClassPathResource(location).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("cannot load Redis script: " + location, exception);
        }
    }

    public record PublishCommand(
            String tenantId,
            String userId,
            String sessionId,
            long authVersion,
            long sessionVersion,
            long policyVersion,
            SessionSnapshotProjector.Projection projection
    ) {
    }

    public record PublishResult(boolean changed, String checksum) {
    }
}
