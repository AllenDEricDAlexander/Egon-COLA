package top.egon.cola.component.ddc.admin.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.core.io.ClassPathResource;
import top.egon.cola.component.ddc.common.DdcKeys;
import top.egon.cola.component.ddc.model.dto.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.dto.DdcPublishTarget;
import top.egon.cola.component.ddc.model.enums.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.enums.DdcLeaseRole;
import top.egon.cola.component.ddc.model.vo.DdcInstanceIdentity;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DdcConfigLeaseRedisRepository {

    private static final String REGISTER_SCRIPT = script("redis/ddc_config_lease_register.lua");

    private static final String HEARTBEAT_SCRIPT = script("redis/ddc_config_lease_heartbeat.lua");

    private static final String DEREGISTER_SCRIPT = script("redis/ddc_config_lease_deregister.lua");

    private static final String EXPIRE_SCRIPT = script("redis/ddc_config_lease_expire.lua");

    private final RedissonClient redissonClient;

    private final ObjectMapper objectMapper;

    public DdcConfigLeaseRedisRepository(RedissonClient redissonClient, ObjectMapper objectMapper) {
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
    }

    public void register(DdcInstanceIdentity identity,
                         DdcLeaseSession session,
                         Instant lastHeartbeatAt) {
        Number result = redissonClient.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                REGISTER_SCRIPT,
                RScript.ReturnType.INTEGER,
                keys(identity.bizCode(), identity.env(), identity.appCode(), identity.instanceId()),
                toJson(identity, session, lastHeartbeatAt),
                identity.instanceId(),
                session.leaseSeconds()
        );
        if (result == null || result.intValue() != 1) {
            throw new IllegalStateException("DDC config lease registration failed");
        }
    }

    public DdcLeaseOperationResult heartbeat(DdcHeartbeatRequest request, Instant heartbeatAt) {
        List<?> result = redissonClient.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                HEARTBEAT_SCRIPT,
                RScript.ReturnType.MULTI,
                keys(request.getBizCode(), request.getEnv(), request.getAppCode(), request.getInstanceId()),
                request.getInstanceId(),
                request.getLeaseId(),
                heartbeatAt.toEpochMilli()
        );
        int code = number(result, 0).intValue();
        if (code == 1) {
            return new DdcLeaseOperationResult(
                    DdcLeaseOperationStatus.RENEWED,
                    Instant.ofEpochMilli(number(result, 1).longValue())
            );
        }
        if (code == 2) {
            return new DdcLeaseOperationResult(DdcLeaseOperationStatus.NOT_FOUND, null);
        }
        if (code == 3) {
            return new DdcLeaseOperationResult(DdcLeaseOperationStatus.LEASE_MISMATCH, null);
        }
        throw new IllegalStateException("invalid DDC heartbeat script status: " + code);
    }

    public DdcLeaseOperationResult deregister(DdcHeartbeatRequest request) {
        Number result = redissonClient.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                DEREGISTER_SCRIPT,
                RScript.ReturnType.INTEGER,
                keys(request.getBizCode(), request.getEnv(), request.getAppCode(), request.getInstanceId()),
                request.getInstanceId(),
                request.getLeaseId()
        );
        if (result.intValue() == 1) {
            return new DdcLeaseOperationResult(DdcLeaseOperationStatus.DELETED, null);
        }
        if (result.intValue() == 2) {
            return new DdcLeaseOperationResult(DdcLeaseOperationStatus.NOT_FOUND, null);
        }
        if (result.intValue() == 3) {
            return new DdcLeaseOperationResult(DdcLeaseOperationStatus.NOT_DELETED, null);
        }
        throw new IllegalStateException("invalid DDC deregistration script status: " + result);
    }

    public boolean removeExpiredProjection(String bizCode,
                                           String env,
                                           String appCode,
                                           String instanceId,
                                           String leaseId,
                                           Instant now) {
        Number result = redissonClient.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                EXPIRE_SCRIPT,
                RScript.ReturnType.INTEGER,
                keys(bizCode, env, appCode, instanceId),
                instanceId,
                leaseId,
                now.toEpochMilli()
        );
        if (result == null || (result.intValue() != 0 && result.intValue() != 1)) {
            throw new IllegalStateException("invalid DDC lease expiry script status: " + result);
        }
        return result.intValue() == 1;
    }

    public List<DdcPublishTarget> activeTargets(String bizCode,
                                                String env,
                                                String appCode,
                                                Instant now) {
        Set<String> instanceIds =
                redissonClient.<String>getSet(
                                DdcKeys.v3ConfigLeaseInstances(bizCode, env, appCode),
                                StringCodec.INSTANCE
                        )
                        .readAll();
        List<DdcPublishTarget> targets = new ArrayList<>();
        for (String instanceId : instanceIds) {
            JsonNode lease = currentLease(bizCode, env, appCode, instanceId);
            if (!isActive(lease, bizCode, env, appCode, now)) {
                redissonClient.<String>getSet(
                                DdcKeys.v3ConfigLeaseInstances(bizCode, env, appCode),
                                StringCodec.INSTANCE
                        )
                        .remove(instanceId);
                continue;
            }
            targets.add(new DdcPublishTarget(
                    lease.path("instanceId").asText(),
                    lease.path("leaseId").asText()
            ));
        }
        return targets.stream()
                .sorted(Comparator.comparing(DdcPublishTarget::instanceId)
                        .thenComparing(DdcPublishTarget::leaseId))
                .toList();
    }

    public boolean isActiveTarget(String bizCode,
                                  String env,
                                  String appCode,
                                  DdcPublishTarget target,
                                  Instant now) {
        JsonNode lease = currentLease(
                bizCode, env, appCode, target.instanceId()
        );
        return isActive(lease, bizCode, env, appCode, now)
                && target.leaseId().equals(lease.path("leaseId").asText());
    }

    private List<Object> keys(
            String bizCode, String env, String appCode, String instanceId) {
        return List.of(
                DdcKeys.v3ConfigLeaseInstance(
                        bizCode,
                        env,
                        appCode,
                        instanceId
                ),
                DdcKeys.v3ConfigLeaseInstances(bizCode, env, appCode)
        );
    }

    private String toJson(DdcInstanceIdentity identity,
                          DdcLeaseSession session,
                          Instant lastHeartbeatAt) {
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
        value.put("status", "ONLINE");
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serialize DDC config lease failed", e);
        }
    }

    private JsonNode currentLease(String bizCode,
                                  String env,
                                  String appCode,
                                  String instanceId) {
        String value = redissonClient.<String>getBucket(
                DdcKeys.v3ConfigLeaseInstance(bizCode, env, appCode, instanceId),
                StringCodec.INSTANCE
        ).get();
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("deserialize DDC config lease failed", exception);
        }
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
                && lease.path("leaseExpireAt").asLong() > now.toEpochMilli();
    }

    private Number number(List<?> result, int index) {
        if (result == null || result.size() <= index || !(result.get(index) instanceof Number number)) {
            throw new IllegalStateException("invalid DDC lease script result");
        }
        return number;
    }

    private static String script(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("load DDC lease script failed: " + path, e);
        }
    }
}
