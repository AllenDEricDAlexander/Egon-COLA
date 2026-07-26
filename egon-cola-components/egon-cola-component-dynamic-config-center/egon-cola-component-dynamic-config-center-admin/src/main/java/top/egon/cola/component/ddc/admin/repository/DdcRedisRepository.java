package top.egon.cola.component.ddc.admin.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RBucket;
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

    public void writeConfig(String appCode, String env, String namespace, String key, String value, Long version) {
        redissonClient.<String>getBucket(DdcKeys.v2Config(appCode, env, namespace, key)).set(value);
        redissonClient.<Long>getBucket(DdcKeys.v2Version(appCode, env, namespace, key)).set(version);
        redissonClient.<String>getBucket(DdcKeys.config(appCode, env, namespace, key)).set(value);
        redissonClient.<Long>getBucket(DdcKeys.version(appCode, env, namespace, key)).set(version);
    }

    public void dispatch(DdcAtomicPublishCommand command) {
        List<?> result = redissonClient.getScript().eval(
                RScript.Mode.READ_WRITE,
                PUBLISH_SCRIPT,
                RScript.ReturnType.MULTI,
                List.of(
                        DdcKeys.v2Config(
                                command.appCode(),
                                command.env(),
                                command.namespace(),
                                command.configKey()
                        ),
                        DdcKeys.v2Version(
                                command.appCode(),
                                command.env(),
                                command.namespace(),
                                command.configKey()
                        ),
                        DdcKeys.v2PublishIdempotency(
                                command.appCode(),
                                command.env(),
                                command.namespace(),
                                command.changeId()
                        ),
                        DdcKeys.v2Topic(
                                command.appCode(),
                                command.env(),
                                command.namespace()
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
        writeLegacyConfig(command);
        publishLegacy(command.message());
    }

    public String readConfigValue(String appCode, String env, String namespace, String key) {
        String value = redissonClient.<String>getBucket(
                DdcKeys.v2Config(appCode, env, namespace, key)
        ).get();
        if (value != null) {
            return value;
        }
        RBucket<String> legacy = redissonClient.getBucket(
                DdcKeys.config(appCode, env, namespace, key)
        );
        return legacy.get();
    }

    public Long readConfigVersion(String appCode, String env, String namespace, String key) {
        Long version = redissonClient.<Long>getBucket(
                DdcKeys.v2Version(appCode, env, namespace, key)
        ).get();
        if (version != null) {
            return version;
        }
        RBucket<Long> legacy = redissonClient.getBucket(
                DdcKeys.version(appCode, env, namespace, key)
        );
        return legacy.get();
    }

    public void publish(DdcPublishMessage message) {
        redissonClient.getTopic(DdcKeys.v2Topic(
                message.getAppCode(), message.getEnv(), message.getNamespace()
        )).publish(message);
        redissonClient.getTopic(DdcKeys.topic(message.getAppCode(), message.getEnv(), message.getNamespace()))
                .publish(message);
    }

    private void writeLegacyConfig(DdcAtomicPublishCommand command) {
        redissonClient.<String>getBucket(DdcKeys.config(
                command.appCode(),
                command.env(),
                command.namespace(),
                command.configKey()
        )).set(command.content());
        redissonClient.<Long>getBucket(DdcKeys.version(
                command.appCode(),
                command.env(),
                command.namespace(),
                command.configKey()
        )).set(command.targetVersion());
    }

    private void publishLegacy(DdcPublishMessage message) {
        redissonClient.getTopic(DdcKeys.topic(
                message.getAppCode(),
                message.getEnv(),
                message.getNamespace()
        )).publish(message);
    }

    public void writeInstanceHeartbeat(DdcHeartbeatRequest request) {
        redissonClient.<String>getBucket(DdcKeys.instance(request.getAppCode(), request.getEnv(), request.getNamespace(), request.getInstanceId()))
                .set(toJson(request));
        instances(request.getAppCode(), request.getEnv(), request.getNamespace()).add(request.getInstanceId());
    }

    public void removeInstance(String appCode, String env, String namespace, String instanceId) {
        redissonClient.getBucket(DdcKeys.instance(appCode, env, namespace, instanceId)).delete();
        instances(appCode, env, namespace).remove(instanceId);
    }

    private String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serialize ddc value failed", e);
        }
    }

    private RSet<String> instances(String appCode, String env, String namespace) {
        return redissonClient.getSet(DdcKeys.instances(appCode, env, namespace));
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
