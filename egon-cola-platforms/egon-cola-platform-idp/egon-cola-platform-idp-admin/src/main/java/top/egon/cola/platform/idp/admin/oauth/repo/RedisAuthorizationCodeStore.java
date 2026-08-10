package top.egon.cola.platform.idp.admin.oauth.repo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import top.egon.cola.platform.idp.core.oauth.AuthorizationCode;
import top.egon.cola.platform.idp.core.port.AuthorizationCodeStore;

import java.time.Duration;
import java.util.Objects;
import java.util.regex.Pattern;

public final class RedisAuthorizationCodeStore
        implements AuthorizationCodeStore {

    private static final Pattern DIGEST = Pattern.compile(
            "[A-Za-z0-9_-]{43}"
    );
    private static final Duration MAXIMUM_TTL = Duration.ofMinutes(5);

    private final RedissonClient redisson;
    private final ObjectMapper objectMapper;
    private final String keyPrefix;

    public RedisAuthorizationCodeStore(
            RedissonClient redisson,
            ObjectMapper objectMapper,
            String keyPrefix
    ) {
        this.redisson = Objects.requireNonNull(redisson, "redisson");
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
        this.keyPrefix = validPrefix(keyPrefix);
    }

    @Override
    public void put(
            String codeDigest,
            AuthorizationCode code,
            Duration ttl
    ) {
        Objects.requireNonNull(code, "code");
        Duration validTtl = validTtl(ttl);
        String encoded = encode(code);
        boolean created = bucket(codeDigest).setIfAbsent(encoded, validTtl);
        if (!created) {
            throw new IllegalStateException(
                    "authorization code digest collision"
            );
        }
    }

    @Override
    public AuthorizationCode consume(String codeDigest) {
        String encoded = bucket(codeDigest).getAndDelete();
        return encoded == null ? null : decode(encoded);
    }

    private RBucket<String> bucket(String codeDigest) {
        if (codeDigest == null || !DIGEST.matcher(codeDigest).matches()) {
            throw new IllegalArgumentException("invalid codeDigest");
        }
        return redisson.getBucket(
                keyPrefix + codeDigest,
                StringCodec.INSTANCE
        );
    }

    private String encode(AuthorizationCode code) {
        try {
            return objectMapper.writeValueAsString(code);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "cannot encode authorization code",
                    exception
            );
        }
    }

    private AuthorizationCode decode(String encoded) {
        try {
            return objectMapper.readValue(encoded, AuthorizationCode.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "cannot decode authorization code",
                    exception
            );
        }
    }

    private static Duration validTtl(Duration ttl) {
        Objects.requireNonNull(ttl, "ttl");
        if (ttl.isZero() || ttl.isNegative() || ttl.compareTo(MAXIMUM_TTL) > 0) {
            throw new IllegalArgumentException("authorization code TTL invalid");
        }
        return ttl;
    }

    private static String validPrefix(String value) {
        if (value == null
                || value.isBlank()
                || !value.endsWith(":")
                || value.contains(" ")) {
            throw new IllegalArgumentException("invalid authorization key prefix");
        }
        return value;
    }
}
