package top.egon.cola.platform.idp.starter.state;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import top.egon.cola.platform.idp.contract.IdentityUserState;

import java.util.Objects;
import java.util.Optional;

/**
 * 从共享 Redis 控制面键空间读取 IdP 用户实时状态。
 * 状态投影至少包含主体标识、启停状态和令牌版本，供验证器执行即时失效检查。
 *
 * <p>Reads current IdP user state from the shared Redis control-plane key space. The projection
 * contains at least the subject, activation status, and token version used by the verifier for
 * immediate invalidation checks.</p>
 */
public final class RedisIdentityUserStateReader
        implements IdentityUserStateReader {

    /**
     * 访问身份状态键空间的 Redisson 客户端。
     *
     * <p>Redisson client accessing the identity-state key space.</p>
     */
    private final RedissonClient redisson;

    /**
     * 用户状态 JSON 反序列化器。
     *
     * <p>JSON deserializer for user-state projections.</p>
     */
    private final ObjectMapper objectMapper;

    /**
     * 用户状态 Redis 键前缀。
     *
     * <p>Redis key prefix for user-state projections.</p>
     */
    private final String keyPrefix;

    /**
     * 创建 Redis 用户状态读取器。
     *
     * <p>Creates the Redis-backed user-state reader.</p>
     *
     * @param redisson Redis 客户端；Redis client
     * @param objectMapper 用户状态 JSON 反序列化器；user-state JSON deserializer
     * @param keyPrefix 用户状态键前缀；user-state key prefix
     */
    public RedisIdentityUserStateReader(
            RedissonClient redisson,
            ObjectMapper objectMapper,
            String keyPrefix
    ) {
        this.redisson = Objects.requireNonNull(redisson, "redisson");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        if (keyPrefix == null || keyPrefix.isBlank()) {
            throw new IllegalArgumentException("keyPrefix is required");
        }
        this.keyPrefix = keyPrefix.trim();
    }

    /**
     * 读取并校验指定主体的用户状态投影。
     *
     * <p>Reads and validates the user-state projection for the given subject.</p>
     *
     * @param subject 统一用户主体标识；unified user subject
     * @return 用户状态；键不存在或内容为空时返回空；user state, or empty when the key is absent
     *         or blank
     * @throws IdentityStateUnavailableException 当主体无效、状态 JSON 无效或主体不匹配时；when
     *                                           the subject is invalid, the state JSON is invalid,
     *                                           or the stored subject does not match
     */
    @Override
    public Optional<IdentityUserState> read(String subject) {
        if (subject == null || subject.isBlank()) {
            throw new IdentityStateUnavailableException("subject is required");
        }
        RBucket<String> bucket = redisson.getBucket(
                keyPrefix + subject.trim(), StringCodec.INSTANCE);
        String value = bucket.get();
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            IdentityUserState state = objectMapper.readValue(
                    value, IdentityUserState.class);
            if (!state.subject().equals(subject.trim())) {
                throw new IdentityStateUnavailableException(
                        "identity state subject mismatch");
            }
            return Optional.of(state);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new IdentityStateUnavailableException(
                    "identity state is invalid", exception);
        }
    }

    /**
     * 表示用户实时状态无法可靠读取或内容不可信。
     *
     * <p>Signals that current user state cannot be read reliably or its content cannot be
     * trusted.</p>
     */
    public static final class IdentityStateUnavailableException
            extends RuntimeException {

        /**
         * 使用失败原因创建异常。
         *
         * <p>Creates an exception with a failure reason.</p>
         *
         * @param message 失败原因；failure reason
         */
        public IdentityStateUnavailableException(String message) {
            super(message);
        }

        /**
         * 使用失败原因与底层异常创建异常。
         *
         * <p>Creates an exception with a failure reason and underlying cause.</p>
         *
         * @param message 失败原因；failure reason
         * @param cause 底层异常；underlying cause
         */
        public IdentityStateUnavailableException(
                String message,
                Throwable cause
        ) {
            super(message, cause);
        }
    }
}
