package top.egon.cola.component.ddc.admin.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import top.egon.cola.component.ddc.admin.security.admission.DdcAdmissionClaims;
import top.egon.cola.component.ddc.admin.security.admission.DdcAdmissionException;
import top.egon.cola.component.ddc.error.DdcErrorStatus;
import top.egon.cola.component.ddc.redis.DdcRedisKeys;
import top.egon.cola.component.ddc.model.config.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.config.DdcPublishTarget;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.model.instance.DdcInstanceIdentity;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DdcConfigLeaseRedisRepository {

    private final RedissonClient redissonClient;

    private final ObjectMapper objectMapper;

    public DdcConfigLeaseRedisRepository(RedissonClient redissonClient, ObjectMapper objectMapper) {
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
    }

    public void register(DdcInstanceIdentity identity,
                         DdcLeaseSession session,
                         Instant lastHeartbeatAt,
                         DdcAdmissionClaims admission) {
        RLock lock = scopeLock(identity.bizCode(), identity.env(), identity.appCode());
        lock.lock();
        try {
            lease(identity.bizCode(), identity.env(), identity.appCode(), identity.instanceId())
                    .set(toJson(identity, session, lastHeartbeatAt, admission),
                            ttl(lastHeartbeatAt, session.leaseExpireAt()));
            instances(identity.bizCode(), identity.env(), identity.appCode())
                    .add(identity.instanceId());
        } finally {
            lock.unlock();
        }
    }

    public DdcLeaseOperationResult heartbeat(
            DdcHeartbeatRequest request,
            DdcAdmissionClaims admission,
            Instant heartbeatAt) {
        RLock lock = scopeLock(request.getBizCode(), request.getEnv(), request.getAppCode());
        lock.lock();
        try {
            JsonNode current = currentLease(
                    request.getBizCode(), request.getEnv(), request.getAppCode(), request.getInstanceId());
            if (current == null) {
                return new DdcLeaseOperationResult(DdcLeaseOperationStatus.NOT_FOUND, null);
            }
            if (!sameIdentity(current, request.getInstanceId(), request.getLeaseId())) {
                return new DdcLeaseOperationResult(DdcLeaseOperationStatus.LEASE_MISMATCH, null);
            }
            if (!sameAdmissionResource(current, admission)) {
                throw new DdcAdmissionException(
                        DdcErrorStatus.RESOURCE_ADMISSION_BINDING_MISMATCH
                );
            }
            long leaseSeconds = current.path("leaseSeconds").asLong();
            Instant requestedExpireAt = heartbeatAt.plusSeconds(leaseSeconds);
            Instant leaseExpireAt = requestedExpireAt.isBefore(admission.expiresAt())
                    ? requestedExpireAt : admission.expiresAt();
            ObjectNode renewed = (ObjectNode) current;
            renewed.put("lastHeartbeatAt", heartbeatAt.toEpochMilli());
            renewed.put("leaseExpireAt", leaseExpireAt.toEpochMilli());
            renewed.put("resourceVersion", admission.resourceVersion());
            renewed.put("credentialId", admission.credentialId());
            renewed.put("admissionExpiresAt", admission.expiresAt().toEpochMilli());
            renewed.put("status", "ONLINE");
            lease(request.getBizCode(), request.getEnv(), request.getAppCode(), request.getInstanceId())
                    .set(json(renewed), ttl(heartbeatAt, leaseExpireAt));
            instances(request.getBizCode(), request.getEnv(), request.getAppCode())
                    .add(request.getInstanceId());
            return new DdcLeaseOperationResult(DdcLeaseOperationStatus.RENEWED, leaseExpireAt);
        } finally {
            lock.unlock();
        }
    }

    public DdcLeaseOperationResult deregister(DdcHeartbeatRequest request) {
        RLock lock = scopeLock(request.getBizCode(), request.getEnv(), request.getAppCode());
        lock.lock();
        try {
            JsonNode current = currentLease(
                    request.getBizCode(), request.getEnv(), request.getAppCode(), request.getInstanceId());
            if (current == null) {
                return new DdcLeaseOperationResult(DdcLeaseOperationStatus.NOT_FOUND, null);
            }
            if (!sameIdentity(current, request.getInstanceId(), request.getLeaseId())) {
                return new DdcLeaseOperationResult(DdcLeaseOperationStatus.NOT_DELETED, null);
            }
            lease(request.getBizCode(), request.getEnv(), request.getAppCode(), request.getInstanceId())
                    .delete();
            instances(request.getBizCode(), request.getEnv(), request.getAppCode())
                    .remove(request.getInstanceId());
            return new DdcLeaseOperationResult(DdcLeaseOperationStatus.DELETED, null);
        } finally {
            lock.unlock();
        }
    }

    public boolean removeExpiredProjection(String bizCode,
                                           String env,
                                           String appCode,
                                           String instanceId,
                                           String leaseId,
                                           Instant now) {
        RLock lock = scopeLock(bizCode, env, appCode);
        lock.lock();
        try {
            JsonNode current = currentLease(bizCode, env, appCode, instanceId);
            if (current != null) {
                if (!sameIdentity(current, instanceId, leaseId)
                        || current.path("leaseExpireAt").asLong() > now.toEpochMilli()) {
                    return false;
                }
                lease(bizCode, env, appCode, instanceId).delete();
            }
            instances(bizCode, env, appCode).remove(instanceId);
            return true;
        } finally {
            lock.unlock();
        }
    }

    public List<DdcPublishTarget> activeTargets(String bizCode,
                                                String env,
                                                String appCode,
                                                Instant now) {
        RLock lock = scopeLock(bizCode, env, appCode);
        lock.lock();
        try {
            Set<String> instanceIds = instances(bizCode, env, appCode).readAll();
            List<DdcPublishTarget> targets = new ArrayList<>();
            for (String instanceId : instanceIds) {
                JsonNode current = currentLease(bizCode, env, appCode, instanceId);
                if (!isActive(current, bizCode, env, appCode, now)) {
                    instances(bizCode, env, appCode).remove(instanceId);
                    continue;
                }
                targets.add(new DdcPublishTarget(
                        current.path("instanceId").asText(),
                        current.path("leaseId").asText()
                ));
            }
            return targets.stream()
                    .sorted(Comparator.comparing(DdcPublishTarget::instanceId)
                            .thenComparing(DdcPublishTarget::leaseId))
                    .toList();
        } finally {
            lock.unlock();
        }
    }

    public boolean isActiveTarget(String bizCode,
                                  String env,
                                  String appCode,
                                  DdcPublishTarget target,
                                  Instant now) {
        RLock lock = scopeLock(bizCode, env, appCode);
        lock.lock();
        try {
            JsonNode current = currentLease(bizCode, env, appCode, target.instanceId());
            return isActive(current, bizCode, env, appCode, now)
                    && target.leaseId().equals(current.path("leaseId").asText());
        } finally {
            lock.unlock();
        }
    }

    private RBucket<String> lease(String bizCode, String env, String appCode, String instanceId) {
        return redissonClient.getBucket(
                DdcRedisKeys.configLeaseInstance(bizCode, env, appCode, instanceId),
                StringCodec.INSTANCE
        );
    }

    private RSet<String> instances(String bizCode, String env, String appCode) {
        return redissonClient.getSet(
                DdcRedisKeys.configLeaseInstances(bizCode, env, appCode),
                StringCodec.INSTANCE
        );
    }

    private RLock scopeLock(String bizCode, String env, String appCode) {
        return redissonClient.getLock(
                DdcRedisKeys.configLeaseInstances(bizCode, env, appCode) + ":lock");
    }

    private String toJson(DdcInstanceIdentity identity,
                          DdcLeaseSession session,
                          Instant lastHeartbeatAt,
                          DdcAdmissionClaims admission) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("instanceId", identity.instanceId());
        value.put("leaseId", session.leaseId());
        value.put("role", session.role().name());
        value.put("bizCode", identity.bizCode());
        value.put("appCode", identity.appCode());
        value.put("env", identity.env());
        value.put("host", identity.host());
        value.put("port", identity.port());
        value.put("pid", identity.pid());
        value.put("sdkVersion", identity.sdkVersion());
        value.put("leaseSeconds", session.leaseSeconds());
        value.put("heartbeatIntervalSeconds", session.heartbeatIntervalSeconds());
        value.put("registeredAt", session.registeredAt().toEpochMilli());
        value.put("lastHeartbeatAt", lastHeartbeatAt.toEpochMilli());
        value.put("leaseExpireAt", session.leaseExpireAt().toEpochMilli());
        value.put("resourceServerId", admission.resourceServerId());
        value.put("resourceVersion", admission.resourceVersion());
        value.put("credentialId", admission.credentialId());
        value.put("admissionExpiresAt", admission.expiresAt().toEpochMilli());
        value.put("status", "ONLINE");
        return json(value);
    }

    private JsonNode currentLease(String bizCode,
                                  String env,
                                  String appCode,
                                  String instanceId) {
        String value = lease(bizCode, env, appCode, instanceId).get();
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("deserialize DDC config lease failed", exception);
        }
    }

    private boolean sameIdentity(JsonNode lease, String instanceId, String leaseId) {
        return instanceId.equals(lease.path("instanceId").asText())
                && leaseId.equals(lease.path("leaseId").asText());
    }

    private boolean sameAdmissionResource(
            JsonNode lease,
            DdcAdmissionClaims admission
    ) {
        return admission.resourceServerId().equals(
                lease.path("resourceServerId").asText()
        );
    }

    private boolean isActive(JsonNode lease,
                             String bizCode,
                             String env,
                             String appCode,
                             Instant now) {
        return lease != null
                && lease.hasNonNull("instanceId")
                && lease.hasNonNull("leaseId")
                && DdcLeaseRole.CONFIG_CLIENT.name().equals(lease.path("role").asText())
                && bizCode.equals(lease.path("bizCode").asText())
                && appCode.equals(lease.path("appCode").asText())
                && env.equals(lease.path("env").asText())
                && "ONLINE".equals(lease.path("status").asText())
                && lease.path("leaseExpireAt").canConvertToLong()
                && lease.path("leaseExpireAt").asLong() > now.toEpochMilli()
                && lease.hasNonNull("resourceServerId")
                && lease.path("resourceVersion").canConvertToLong()
                && lease.path("resourceVersion").asLong() > 0
                && lease.hasNonNull("credentialId")
                && lease.path("admissionExpiresAt").canConvertToLong()
                && lease.path("admissionExpiresAt").asLong() > now.toEpochMilli();
    }

    private Duration ttl(Instant now, Instant expiresAt) {
        long seconds = Math.max(1L, Duration.between(now, expiresAt).toSeconds());
        return Duration.ofSeconds(seconds);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("serialize DDC config lease failed", exception);
        }
    }
}
