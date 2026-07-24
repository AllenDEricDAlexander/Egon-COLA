package top.egon.cola.component.ddc.admin.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import top.egon.cola.component.ddc.common.DdcKeys;
import top.egon.cola.component.ddc.model.dto.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.enums.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.enums.DdcLeaseRole;
import top.egon.cola.component.ddc.model.vo.DdcInstanceIdentity;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        Number result = redissonClient.getScript().eval(
                RScript.Mode.READ_WRITE,
                REGISTER_SCRIPT,
                RScript.ReturnType.INTEGER,
                keys(identity.env(), identity.namespace(), identity.appCode(), identity.instanceId()),
                toJson(identity, session, lastHeartbeatAt),
                identity.instanceId(),
                session.leaseSeconds()
        );
        if (result == null || result.intValue() != 1) {
            throw new IllegalStateException("DDC config lease registration failed");
        }
    }

    public DdcLeaseOperationResult heartbeat(DdcHeartbeatRequest request, Instant heartbeatAt) {
        List<?> result = redissonClient.getScript().eval(
                RScript.Mode.READ_WRITE,
                HEARTBEAT_SCRIPT,
                RScript.ReturnType.MULTI,
                keys(request.getEnv(), request.getNamespace(), request.getAppCode(), request.getInstanceId()),
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
        Number result = redissonClient.getScript().eval(
                RScript.Mode.READ_WRITE,
                DEREGISTER_SCRIPT,
                RScript.ReturnType.INTEGER,
                keys(request.getEnv(), request.getNamespace(), request.getAppCode(), request.getInstanceId()),
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

    public boolean removeExpiredProjection(String appCode,
                                           String env,
                                           String namespace,
                                           String instanceId,
                                           String leaseId,
                                           Instant now) {
        Number result = redissonClient.getScript().eval(
                RScript.Mode.READ_WRITE,
                EXPIRE_SCRIPT,
                RScript.ReturnType.INTEGER,
                keys(env, namespace, appCode, instanceId),
                instanceId,
                leaseId,
                now.toEpochMilli()
        );
        if (result == null || (result.intValue() != 0 && result.intValue() != 1)) {
            throw new IllegalStateException("invalid DDC lease expiry script status: " + result);
        }
        return result.intValue() == 1;
    }

    private List<Object> keys(String env, String namespace, String appCode, String instanceId) {
        return List.of(
                DdcKeys.leaseInstance(
                        env,
                        namespace,
                        DdcLeaseRole.CONFIG_CLIENT,
                        instanceId
                ),
                DdcKeys.instances(appCode, env, namespace)
        );
    }

    private String toJson(DdcInstanceIdentity identity,
                          DdcLeaseSession session,
                          Instant lastHeartbeatAt) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("instanceId", identity.instanceId());
        value.put("leaseId", session.leaseId());
        value.put("role", session.role().name());
        value.put("appCode", identity.appCode());
        value.put("env", identity.env());
        value.put("namespace", identity.namespace());
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
