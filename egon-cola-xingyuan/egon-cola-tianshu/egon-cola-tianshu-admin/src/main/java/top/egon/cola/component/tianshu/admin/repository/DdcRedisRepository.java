package top.egon.cola.component.tianshu.admin.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.tianshu.admin.common.DdcAdminException;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAtomicPublishCommand;
import top.egon.cola.component.tianshu.error.DdcErrorStatus;
import top.egon.cola.component.tianshu.format.DdcChecksum;
import top.egon.cola.component.tianshu.redis.DdcRedisKeys;
import top.egon.cola.component.tianshu.model.config.DdcHeartbeatRequest;
import top.egon.cola.component.tianshu.model.config.DdcPublishMessage;
import top.egon.cola.component.tianshu.model.event.DdcConfigDispatchPreparedEvent;
import top.egon.cola.component.tianshu.model.tenant.DdcTenantMessageDTO;

import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class DdcRedisRepository {

    @Qualifier("ddcAdminRedissonClient")
    private final RedissonClient redissonClient;

    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    public void writeConfig(
            String bizCode, String env, String appCode,
            String key, String value, Long version) {
        long tenantId = requiredTenantId();
        RLock lock = configLock(tenantId, bizCode, env, appCode);
        lock.lock();
        try {
            redissonClient.<String>getBucket(
                    DdcRedisKeys.config(tenantId, bizCode, env, appCode, key)).set(value);
            redissonClient.<Long>getBucket(
                    DdcRedisKeys.version(tenantId, bizCode, env, appCode, key)).set(version);
        } finally {
            lock.unlock();
        }
    }

    public void dispatch(DdcAtomicPublishCommand command) {
        long tenantId = requiredTenantId();
        dispatch(command, tenantId, command.changeId(), command.changeId() + ":" + command.eventChecksum(),
                new DdcTenantMessageDTO<>(tenantId, command.message()), false);
    }

    public void dispatch(
            DdcAtomicPublishCommand command,
            DdcConfigDispatchPreparedEvent event) {
        long tenantId = requiredTenantId();
        if (command == null || event == null || command.message() == null || !event.hasValidIdentityAndChecksum()
                || !Long.valueOf(tenantId).equals(event.getTenantId())
                || !Long.valueOf(tenantId).equals(command.tenantId())
                || !event.getEventId().equals(command.eventId())
                || !Objects.equals(event.getAttempt(), command.attempt())
                || !event.getMessage().getChangeId().equals(command.changeId())
                || !Objects.equals(command.eventChecksum(), event.getEventChecksum())
                || !Objects.equals(command.configId(), event.getConfigId())
                || !Objects.equals(command.expectedPublishedVersion(), event.getExpectedPublishedVersion())
                || !Objects.equals(command.targetVersion(), event.getMessage().getTargetVersion())
                || !Objects.equals(command.content(), event.getMessage().getContent())
                || !Objects.equals(command.bizCode(), event.getMessage().getBizCode())
                || !Objects.equals(command.env(), event.getMessage().getEnv())
                || !Objects.equals(command.appCode(), event.getMessage().getAppCode())
                || !Objects.equals(command.resourceName(), event.getMessage().getResourceName())
                || !Objects.equals(command.message().getChecksum(), event.getMessage().getChecksum())
                || !Objects.equals(command.message().getPublishMode(), event.getMessage().getPublishMode())
                || !Objects.equals(command.message().getOperator(), event.getMessage().getOperator())
                || !Objects.equals(command.message().getTimestamp(), event.getMessage().getTimestamp())
                || !Objects.equals(command.message().getChecksum(), DdcChecksum.sha256(command.message()))
                || !Objects.equals(event.getMessage().getChecksum(), DdcChecksum.sha256(event.getMessage()))) {
            throw new DdcAdminException("prepared config event does not match the trusted dispatch command");
        }
        dispatch(command, tenantId, event.getEventId(),
                event.getEventId() + ":" + event.getEventChecksum(),
                new DdcTenantMessageDTO<>(tenantId, event), true);
    }

    private void dispatch(
            DdcAtomicPublishCommand command,
            long tenantId,
            String idempotencyIdentity,
            String fingerprint,
            DdcTenantMessageDTO<?> envelope,
            boolean republishMatchingEvent) {
        RLock lock = configLock(tenantId, command.bizCode(), command.env(), command.appCode());
        lock.lock();
        try {
            String configKey = DdcRedisKeys.config(
                    tenantId, command.bizCode(), command.env(), command.appCode(), command.resourceName());
            String versionKey = DdcRedisKeys.version(
                    tenantId, command.bizCode(), command.env(), command.appCode(), command.resourceName());
            String idempotencyKey = DdcRedisKeys.publishIdempotency(
                    tenantId, command.bizCode(), command.env(), command.appCode(), idempotencyIdentity);
            String existing = redissonClient.<String>getBucket(idempotencyKey).get();
            if (existing != null) {
                if (!existing.equals(fingerprint)) {
                    throw new DdcAdminException(DdcErrorStatus.CHANGE_ID_CONFLICT);
                }
                if (republishMatchingEvent) {
                    redissonClient.getTopic(DdcRedisKeys.topic(
                            tenantId, command.bizCode(), command.env(), command.appCode()
                    )).publish(envelope);
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
                    tenantId, command.bizCode(), command.env(), command.appCode()
            )).publish(envelope);
        } finally {
            lock.unlock();
        }
    }

    public String readConfigValue(
            String bizCode, String env, String appCode, String key) {
        long tenantId = requiredTenantId();
        return redissonClient.<String>getBucket(
                DdcRedisKeys.config(tenantId, bizCode, env, appCode, key)
        ).get();
    }

    public Long readConfigVersion(
            String bizCode, String env, String appCode, String key) {
        long tenantId = requiredTenantId();
        return redissonClient.<Long>getBucket(
                DdcRedisKeys.version(tenantId, bizCode, env, appCode, key)
        ).get();
    }

    public void publish(DdcPublishMessage message) {
        long tenantId = requiredTenantId();
        redissonClient.getTopic(DdcRedisKeys.topic(
                tenantId,
                message.getBizCode(),
                message.getEnv(),
                message.getAppCode()
        )).publish(new DdcTenantMessageDTO<>(tenantId, message));
    }

    public void writeInstanceHeartbeat(DdcHeartbeatRequest request) {
        long tenantId = requiredTenantId();
        RLock lock = configLock(tenantId, request.getBizCode(), request.getEnv(), request.getAppCode());
        lock.lock();
        try {
            redissonClient.<String>getBucket(DdcRedisKeys.configLeaseInstance(
                            tenantId, request.getBizCode(), request.getEnv(),
                            request.getAppCode(), request.getInstanceId()))
                    .set(toJson(request));
            instances(tenantId, request.getBizCode(), request.getEnv(), request.getAppCode())
                    .add(request.getInstanceId());
        } finally {
            lock.unlock();
        }
    }

    public void removeInstance(
            String bizCode, String env, String appCode, String instanceId) {
        long tenantId = requiredTenantId();
        RLock lock = configLock(tenantId, bizCode, env, appCode);
        lock.lock();
        try {
            redissonClient.getBucket(DdcRedisKeys.configLeaseInstance(
                    tenantId, bizCode, env, appCode, instanceId)).delete();
            instances(tenantId, bizCode, env, appCode).remove(instanceId);
        } finally {
            lock.unlock();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serialize ddc value failed", e);
        }
    }

    private RSet<String> instances(long tenantId, String bizCode, String env, String appCode) {
        return redissonClient.getSet(
                DdcRedisKeys.configLeaseInstances(tenantId, bizCode, env, appCode));
    }

    private RLock configLock(long tenantId, String bizCode, String env, String appCode) {
        return redissonClient.getLock(
                DdcRedisKeys.topic(tenantId, bizCode, env, appCode) + ":lock");
    }

    private long requiredTenantId() {
        String value = MDC.get("tenantId");
        try {
            long tenantId = Long.parseLong(value);
            if (tenantId > 0 && Long.toString(tenantId).equals(value)) {
                return tenantId;
            }
        } catch (NumberFormatException ignored) {
            // A request without trusted tenant MDC is rejected below.
        }
        throw new IllegalStateException("A positive trusted tenantId is required for Tianshu Redis access");
    }
}
