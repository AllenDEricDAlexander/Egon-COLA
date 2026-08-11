package top.egon.cola.platform.idp.admin.oauth.repo;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import top.egon.cola.platform.idp.core.port.ClientAssertionReplayStore;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 使用 Redis 原子 put-if-absent 阻止 Client Assertion 重放。
 *
 * <p>Prevents Client Assertion replay with Redis atomic put-if-absent.</p>
 */
public final class RedisClientAssertionReplayStore
        implements ClientAssertionReplayStore {

    /** Redis Key 段允许的安全字符；safe characters allowed in Redis key segments. */
    private static final Pattern KEY_SEGMENT = Pattern.compile(
            "[A-Za-z0-9._~-]{1,128}"
    );

    /** 身份运行态 Redis；identity-runtime Redis. */
    private final RedissonClient redisson;

    /** Replay Key 前缀；replay-key prefix. */
    private final String keyPrefix;

    /** UTC 业务时钟；UTC business clock. */
    private final Clock clock;

    /**
     * 创建 Redis Assertion 防重放存储。
     *
     * <p>Creates the Redis assertion replay store.</p>
     *
     * @param redisson 身份运行态 Redis；identity-runtime Redis
     * @param keyPrefix Redis Key 前缀；Redis key prefix
     * @param clock UTC 业务时钟；UTC business clock
     */
    public RedisClientAssertionReplayStore(
            RedissonClient redisson,
            String keyPrefix,
            Clock clock
    ) {
        this.redisson = Objects.requireNonNull(redisson, "redisson");
        this.keyPrefix = required(keyPrefix, "keyPrefix");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** {@inheritDoc} */
    @Override
    public boolean markIfAbsent(
            String clientId,
            String tokenId,
            Instant expiresAt
    ) {
        String safeClientId = keySegment(clientId, "clientId");
        String safeTokenId = keySegment(tokenId, "tokenId");
        Instant now = clock.instant();
        Duration ttl = Duration.between(
                now,
                Objects.requireNonNull(expiresAt, "expiresAt")
        );
        if (ttl.isZero() || ttl.isNegative()) {
            return false;
        }
        RBucket<String> bucket = redisson.getBucket(
                keyPrefix + safeClientId + ":" + safeTokenId
        );
        return bucket.setIfAbsent("1", ttl);
    }

    /**
     * 校验 Redis Key 的单一安全段。
     *
     * <p>Validates one safe Redis key segment.</p>
     *
     * @param value 原始值；raw value
     * @param field 字段名；field name
     * @return 已校验值；validated value
     */
    private static String keySegment(String value, String field) {
        String result = required(value, field);
        if (!KEY_SEGMENT.matcher(result).matches()) {
            throw new IllegalArgumentException(
                    field + " contains unsupported characters"
            );
        }
        return result;
    }

    /**
     * 校验必填文本。
     *
     * <p>Validates required text.</p>
     *
     * @param value 待校验值；value to validate
     * @param field 字段名；field name
     * @return 已校验值；validated value
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
