package top.egon.cola.platform.idp.starter.state;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.util.Objects;
import java.util.Optional;

/**
 * 从 IdP Redis 运行态键空间读取 Resource Server 投影。
 *
 * <p>Reads Resource Server projections from the IdP Redis runtime key space.</p>
 */
public final class RedisIdentityResourceServerStateReader
        implements IdentityResourceServerStateReader {

    /** Redis 客户端；Redis client. */
    private final RedissonClient redisson;

    /** JSON 反序列化器；JSON deserializer. */
    private final ObjectMapper objectMapper;

    /** Resource Server 键前缀；Resource Server key prefix. */
    private final String keyPrefix;

    /**
     * 创建 Redis Resource Server 状态读取器。
     *
     * <p>Creates a Redis-backed Resource Server state reader.</p>
     *
     * @param redisson Redis 客户端；Redis client
     * @param objectMapper JSON 反序列化器；JSON deserializer
     * @param keyPrefix Resource Server 键前缀；Resource Server key prefix
     */
    public RedisIdentityResourceServerStateReader(
            RedissonClient redisson,
            ObjectMapper objectMapper,
            String keyPrefix
    ) {
        this.redisson = Objects.requireNonNull(redisson, "redisson");
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
        this.keyPrefix = required(keyPrefix, "keyPrefix");
    }

    /** {@inheritDoc} */
    @Override
    public Optional<IdentityResourceServerState> read(
            String resourceServerId
    ) {
        String expectedId = required(
                resourceServerId,
                "resourceServerId"
        );
        RBucket<String> bucket = redisson.getBucket(
                keyPrefix + expectedId,
                StringCodec.INSTANCE
        );
        String value = bucket.get();
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            IdentityResourceServerState state = objectMapper.readValue(
                    value,
                    IdentityResourceServerState.class
            );
            if (!state.resourceServerId().equals(expectedId)) {
                throw new StateUnavailableException(
                        "Resource Server state identifier mismatch"
                );
            }
            return Optional.of(state);
        } catch (JsonProcessingException | IllegalArgumentException invalid) {
            throw new StateUnavailableException(
                    "Resource Server state is invalid",
                    invalid
            );
        }
    }

    /**
     * 校验必填文本。
     *
     * <p>Validates required text.</p>
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new StateUnavailableException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 表示 Resource Server 投影无法可靠读取。
     *
     * <p>Signals that a Resource Server projection cannot be read reliably.</p>
     */
    public static final class StateUnavailableException
            extends RuntimeException {

        /**
         * 使用失败原因创建异常。
         *
         * <p>Creates an exception with a failure reason.</p>
         *
         * @param message 失败原因；failure reason
         */
        public StateUnavailableException(String message) {
            super(message);
        }

        /**
         * 使用失败原因与底层异常创建异常。
         *
         * <p>Creates an exception with a failure reason and cause.</p>
         *
         * @param message 失败原因；failure reason
         * @param cause 底层异常；underlying cause
         */
        public StateUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
