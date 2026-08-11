package top.egon.cola.platform.idp.starter.state;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.util.Objects;
import java.util.Optional;

/**
 * 从 IdP Redis 运行态键空间读取 OAuth Client 投影。
 *
 * <p>Reads OAuth Client projections from the IdP Redis runtime key space.</p>
 */
public final class RedisIdentityOAuthClientStateReader
        implements IdentityOAuthClientStateReader {

    /** Redis 客户端；Redis client. */
    private final RedissonClient redisson;

    /** JSON 反序列化器；JSON deserializer. */
    private final ObjectMapper objectMapper;

    /** OAuth Client 键前缀；OAuth Client key prefix. */
    private final String keyPrefix;

    /**
     * 创建 Redis OAuth Client 状态读取器。
     *
     * <p>Creates a Redis-backed OAuth Client state reader.</p>
     *
     * @param redisson Redis 客户端；Redis client
     * @param objectMapper JSON 反序列化器；JSON deserializer
     * @param keyPrefix OAuth Client 键前缀；OAuth Client key prefix
     */
    public RedisIdentityOAuthClientStateReader(
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
    public Optional<IdentityOAuthClientState> read(String clientId) {
        String expectedId = required(clientId, "clientId");
        RBucket<String> bucket = redisson.getBucket(
                keyPrefix + expectedId,
                StringCodec.INSTANCE
        );
        String value = bucket.get();
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            IdentityOAuthClientState state = objectMapper.readValue(
                    value,
                    IdentityOAuthClientState.class
            );
            if (!state.clientId().equals(expectedId)) {
                throw new StateUnavailableException(
                        "OAuth Client state identifier mismatch"
                );
            }
            return Optional.of(state);
        } catch (JsonProcessingException | IllegalArgumentException invalid) {
            throw new StateUnavailableException(
                    "OAuth Client state is invalid",
                    invalid
            );
        }
    }

    /** 校验必填文本；Validates required text. */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new StateUnavailableException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 表示 OAuth Client 投影无法可靠读取。
     *
     * <p>Signals that an OAuth Client projection cannot be read reliably.</p>
     */
    public static final class StateUnavailableException
            extends RuntimeException {

        /** @param message 失败原因；failure reason */
        public StateUnavailableException(String message) {
            super(message);
        }

        /**
         * @param message 失败原因；failure reason
         * @param cause 底层异常；underlying cause
         */
        public StateUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
