package top.egon.cola.component.ddc.admin.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RLock;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.admin.model.vo.DdcAtomicPublishCommand;
import top.egon.cola.component.ddc.error.DdcErrorStatus;
import top.egon.cola.component.ddc.transport.redis.DdcRedisKeys;
import top.egon.cola.component.ddc.configuration.model.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.configuration.model.DdcPublishMessage;

public class DdcRedisRepository {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RedissonClient redissonClient;

    public DdcRedisRepository(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public void writeConfig(
            String bizCode, String env, String appCode,
            String key, String value, Long version) {
        RLock lock = configLock(bizCode, env, appCode);
        lock.lock();
        try {
            redissonClient.<String>getBucket(
                    DdcRedisKeys.config(bizCode, env, appCode, key)).set(value);
            redissonClient.<Long>getBucket(
                    DdcRedisKeys.version(bizCode, env, appCode, key)).set(version);
        } finally {
            lock.unlock();
        }
    }

    public void dispatch(DdcAtomicPublishCommand command) {
        RLock lock = configLock(command.bizCode(), command.env(), command.appCode());
        lock.lock();
        try {
            String configKey = DdcRedisKeys.config(
                    command.bizCode(), command.env(), command.appCode(), command.resourceName());
            String versionKey = DdcRedisKeys.version(
                    command.bizCode(), command.env(), command.appCode(), command.resourceName());
            String idempotencyKey = DdcRedisKeys.publishIdempotency(
                    command.bizCode(), command.env(), command.appCode(), command.changeId());
            String fingerprint = command.changeId() + ":" + command.eventChecksum();
            String existing = redissonClient.<String>getBucket(idempotencyKey).get();
            if (existing != null) {
                if (!existing.equals(fingerprint)) {
                    throw new DdcAdminException(DdcErrorStatus.CHANGE_ID_CONFLICT);
                }
                return;
            }

            Long currentVersion = redissonClient.<Long>getBucket(versionKey).get();
            long expectedVersion = command.expectedPublishedVersion() == null
                    ? -1L
                    : command.expectedPublishedVersion();
            if (currentVersion != null
                    && currentVersion != expectedVersion
                    && currentVersion != command.targetVersion()) {
                throw new DdcAdminException("published Redis version changed");
            }
            if (currentVersion != null && currentVersion == command.targetVersion()) {
                String currentValue = redissonClient.<String>getBucket(configKey).get();
                if (currentValue != null && !currentValue.equals(command.content())) {
                    throw new DdcAdminException(DdcErrorStatus.CHANGE_ID_CONFLICT);
                }
            }

            redissonClient.<String>getBucket(configKey).set(command.content());
            redissonClient.<Long>getBucket(versionKey).set(command.targetVersion());
            redissonClient.<String>getBucket(idempotencyKey).set(fingerprint);
            redissonClient.getTopic(DdcRedisKeys.topic(
                    command.bizCode(), command.env(), command.appCode()
            )).publish(command.message());
        } finally {
            lock.unlock();
        }
    }

    public String readConfigValue(
            String bizCode, String env, String appCode, String key) {
        return redissonClient.<String>getBucket(
                DdcRedisKeys.config(bizCode, env, appCode, key)
        ).get();
    }

    public Long readConfigVersion(
            String bizCode, String env, String appCode, String key) {
        return redissonClient.<Long>getBucket(
                DdcRedisKeys.version(bizCode, env, appCode, key)
        ).get();
    }

    public void publish(DdcPublishMessage message) {
        redissonClient.getTopic(DdcRedisKeys.topic(
                message.getBizCode(),
                message.getEnv(),
                message.getAppCode()
        )).publish(message);
    }

    public void writeInstanceHeartbeat(DdcHeartbeatRequest request) {
        RLock lock = configLock(request.getBizCode(), request.getEnv(), request.getAppCode());
        lock.lock();
        try {
            redissonClient.<String>getBucket(DdcRedisKeys.configLeaseInstance(
                            request.getBizCode(), request.getEnv(),
                            request.getAppCode(), request.getInstanceId()))
                    .set(toJson(request));
            instances(request.getBizCode(), request.getEnv(), request.getAppCode())
                    .add(request.getInstanceId());
        } finally {
            lock.unlock();
        }
    }

    public void removeInstance(
            String bizCode, String env, String appCode, String instanceId) {
        RLock lock = configLock(bizCode, env, appCode);
        lock.lock();
        try {
            redissonClient.getBucket(DdcRedisKeys.configLeaseInstance(
                    bizCode, env, appCode, instanceId)).delete();
            instances(bizCode, env, appCode).remove(instanceId);
        } finally {
            lock.unlock();
        }
    }

    private String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serialize ddc value failed", e);
        }
    }

    private RSet<String> instances(String bizCode, String env, String appCode) {
        return redissonClient.getSet(
                DdcRedisKeys.configLeaseInstances(bizCode, env, appCode));
    }

    private RLock configLock(String bizCode, String env, String appCode) {
        return redissonClient.getLock(
                DdcRedisKeys.topic(bizCode, env, appCode) + ":lock");
    }
}
