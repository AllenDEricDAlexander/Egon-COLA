package top.egon.cola.component.ddc.admin.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RScript;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.admin.model.vo.DdcAtomicPublishCommand;
import top.egon.cola.component.ddc.common.DdcKeys;
import top.egon.cola.component.ddc.common.DdcErrorStatus;
import top.egon.cola.component.ddc.model.dto.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.dto.DdcPublishMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class DdcRedisRepository {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String PUBLISH_SCRIPT = script("redis/ddc_config_publish.lua");

    private final RedissonClient redissonClient;

    public DdcRedisRepository(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public void writeConfig(
            String bizCode, String env, String appCode,
            String key, String value, Long version) {
        redissonClient.<String>getBucket(
                DdcKeys.v3Config(bizCode, env, appCode, key)).set(value);
        redissonClient.<Long>getBucket(
                DdcKeys.v3Version(bizCode, env, appCode, key)).set(version);
    }

    public void dispatch(DdcAtomicPublishCommand command) {
        List<?> result = redissonClient.getScript().eval(
                RScript.Mode.READ_WRITE,
                PUBLISH_SCRIPT,
                RScript.ReturnType.MULTI,
                List.of(
                        DdcKeys.v3Config(
                                command.bizCode(),
                                command.env(),
                                command.appCode(),
                                command.configKey()
                        ),
                        DdcKeys.v3Version(
                                command.bizCode(),
                                command.env(),
                                command.appCode(),
                                command.configKey()
                        ),
                        DdcKeys.v3PublishIdempotency(
                                command.bizCode(),
                                command.env(),
                                command.appCode(),
                                command.changeId()
                        ),
                        DdcKeys.v3Topic(
                                command.bizCode(),
                                command.env(),
                                command.appCode()
                        )
                ),
                command.expectedPublishedVersion() == null
                        ? -1L
                        : command.expectedPublishedVersion(),
                command.targetVersion(),
                command.changeId(),
                command.content(),
                command.message(),
                command.eventChecksum()
        );
        int status = resultCode(result);
        if (status == 3) {
            throw new DdcAdminException(DdcErrorStatus.CHANGE_ID_CONFLICT);
        }
        if (status == 4) {
            throw new DdcAdminException("published Redis version changed");
        }
        if (status != 1 && status != 2) {
            throw new IllegalStateException(
                    "invalid DDC config publish script status: " + status
            );
        }
    }

    public String readConfigValue(
            String bizCode, String env, String appCode, String key) {
        return redissonClient.<String>getBucket(
                DdcKeys.v3Config(bizCode, env, appCode, key)
        ).get();
    }

    public Long readConfigVersion(
            String bizCode, String env, String appCode, String key) {
        return redissonClient.<Long>getBucket(
                DdcKeys.v3Version(bizCode, env, appCode, key)
        ).get();
    }

    public void publish(DdcPublishMessage message) {
        redissonClient.getTopic(DdcKeys.v3Topic(
                message.getBizCode(),
                message.getEnv(),
                message.getAppCode()
        )).publish(message);
    }

    public void writeInstanceHeartbeat(DdcHeartbeatRequest request) {
        redissonClient.<String>getBucket(DdcKeys.v3ConfigLeaseInstance(
                        request.getBizCode(), request.getEnv(),
                        request.getAppCode(), request.getInstanceId()))
                .set(toJson(request));
        instances(request.getBizCode(), request.getEnv(), request.getAppCode())
                .add(request.getInstanceId());
    }

    public void removeInstance(
            String bizCode, String env, String appCode, String instanceId) {
        redissonClient.getBucket(DdcKeys.v3ConfigLeaseInstance(
                bizCode, env, appCode, instanceId)).delete();
        instances(bizCode, env, appCode).remove(instanceId);
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
                DdcKeys.v3ConfigLeaseInstances(bizCode, env, appCode));
    }

    private int resultCode(List<?> result) {
        if (result == null || result.isEmpty() || !(result.getFirst() instanceof Number code)) {
            throw new IllegalStateException("invalid DDC config publish script result");
        }
        return code.intValue();
    }

    private static String script(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("load DDC Redis script failed: " + path, exception);
        }
    }
}
