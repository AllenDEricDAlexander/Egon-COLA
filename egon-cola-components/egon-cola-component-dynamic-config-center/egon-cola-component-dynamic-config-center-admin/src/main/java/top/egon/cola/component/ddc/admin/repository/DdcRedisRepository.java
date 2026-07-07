package top.egon.cola.component.ddc.admin.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RBucket;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import top.egon.cola.component.ddc.common.DdcKeys;
import top.egon.cola.component.ddc.model.dto.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.dto.DdcPublishMessage;

public class DdcRedisRepository {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RedissonClient redissonClient;

    public DdcRedisRepository(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public void writeConfig(String appCode, String env, String namespace, String key, String value, Long version) {
        redissonClient.<String>getBucket(DdcKeys.config(appCode, env, namespace, key)).set(value);
        redissonClient.<Long>getBucket(DdcKeys.version(appCode, env, namespace, key)).set(version);
    }

    public String readConfigValue(String appCode, String env, String namespace, String key) {
        RBucket<String> bucket = redissonClient.getBucket(DdcKeys.config(appCode, env, namespace, key));
        return bucket.get();
    }

    public Long readConfigVersion(String appCode, String env, String namespace, String key) {
        RBucket<Long> bucket = redissonClient.getBucket(DdcKeys.version(appCode, env, namespace, key));
        return bucket.get();
    }

    public void publish(DdcPublishMessage message) {
        redissonClient.getTopic(DdcKeys.topic(message.getAppCode(), message.getEnv(), message.getNamespace()))
                .publish(message);
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
}
