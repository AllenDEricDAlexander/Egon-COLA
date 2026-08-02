package top.egon.cola.platform.idp.admin.oauth.infrastructure;

import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

/** Stores opaque browser SSO sessions without exposing identity data in cookies. */
public final class IdpSsoSessionStore {

    private static final Duration MAXIMUM_TTL = Duration.ofDays(30);

    private final RedissonClient redisson;
    private final SecureRandom random;
    private final String keyPrefix;

    public IdpSsoSessionStore(
            RedissonClient redisson,
            SecureRandom random,
            String keyPrefix) {
        this.redisson = Objects.requireNonNull(redisson, "redisson");
        this.random = Objects.requireNonNull(random, "random");
        this.keyPrefix = requiredPrefix(keyPrefix);
    }

    public String create(String identitySub, Duration ttl) {
        String subject = required(identitySub, "identitySub");
        Duration validTtl = validTtl(ttl);
        for (int attempt = 0; attempt < 3; attempt++) {
            byte[] value = new byte[32];
            random.nextBytes(value);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(value);
            if (bucket(token).setIfAbsent(subject, validTtl)) {
                return token;
            }
        }
        throw new IllegalStateException("cannot allocate SSO session");
    }

    public Optional<String> resolve(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(bucket(token).get());
    }

    public void revoke(String token) {
        if (token != null && !token.isBlank()) {
            bucket(token).delete();
        }
    }

    private org.redisson.api.RBucket<String> bucket(String token) {
        return redisson.getBucket(keyPrefix + digest(required(token, "token")),
                StringCodec.INSTANCE);
    }

    private static String digest(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 is unavailable", unavailable);
        }
    }

    private static Duration validTtl(Duration ttl) {
        Objects.requireNonNull(ttl, "ttl");
        if (ttl.isZero() || ttl.isNegative() || ttl.compareTo(MAXIMUM_TTL) > 0) {
            throw new IllegalArgumentException("SSO session TTL is invalid");
        }
        return ttl;
    }

    private static String requiredPrefix(String value) {
        String prefix = required(value, "keyPrefix");
        if (!prefix.endsWith(":")) {
            throw new IllegalArgumentException("keyPrefix must end with ':'");
        }
        return prefix;
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }
}
