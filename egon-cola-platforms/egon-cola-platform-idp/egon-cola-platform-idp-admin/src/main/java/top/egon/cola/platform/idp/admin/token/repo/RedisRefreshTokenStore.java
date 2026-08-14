package top.egon.cola.platform.idp.admin.token.repo;

import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.core.io.ClassPathResource;
import top.egon.cola.platform.idp.core.port.RefreshTokenStore;
import top.egon.cola.platform.idp.core.token.RefreshTokenRecord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Redis-backed stable refresh-token metadata store.
 * 基于 Redis 的稳定 Refresh Token 元数据存储，只保存摘要和索引。
 */
public final class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final Pattern SAFE_SEGMENT = Pattern.compile(
            "[A-Za-z0-9._~-]{1,128}");
    private static final Pattern DIGEST = Pattern.compile(
            "[A-Za-z0-9_-]{43}");
    private static final String SCRIPT_LOCATION = "redis/manage-refresh-token.lua";
    private static final String FIELD_SEPARATOR = "\u001f";

    private final RScript script;
    private final String keyPrefix;
    private final String scriptSource;

    public RedisRefreshTokenStore(RedissonClient redisson, String keyPrefix) {
        Objects.requireNonNull(redisson, "redisson");
        this.script = redisson.getScript(StringCodec.INSTANCE);
        this.keyPrefix = validPrefix(keyPrefix);
        this.scriptSource = loadScript();
    }

    @Override
    public void create(RefreshTokenRecord record) {
        Objects.requireNonNull(record, "record");
        if (record.status() != RefreshTokenRecord.Status.ACTIVE) {
            throw new IllegalArgumentException("new refresh record must be active");
        }
        String result = evaluate(
                List.of(tokenKey(record.tokenDigest()), subjectIndexKey(record.identitySub())),
                "CREATE",
                segment(record.identitySub(), "identitySub"),
                segment(record.tenantId(), "tenantId"),
                millis(record.issuedAt()),
                millis(record.expiresAt()));
        if (!"CREATED".equals(result)) {
            throw new IllegalStateException("refresh token collision");
        }
    }

    @Override
    public Optional<RefreshTokenRecord> findValid(String tokenDigest, Instant now) {
        String result = evaluate(
                List.of(tokenKey(tokenDigest)), "FIND", millis(now));
        if (result == null || result.isBlank()) {
            return Optional.empty();
        }
        String[] fields = result.split(FIELD_SEPARATOR, -1);
        if (fields.length != 5) {
            throw new IllegalStateException("invalid refresh metadata");
        }
        try {
            return Optional.of(new RefreshTokenRecord(
                    validDigest(tokenDigest),
                    fields[0],
                    fields[1],
                    Instant.ofEpochMilli(Long.parseLong(fields[2])),
                    Instant.ofEpochMilli(Long.parseLong(fields[3])),
                    RefreshTokenRecord.Status.valueOf(fields[4])));
        } catch (RuntimeException exception) {
            throw new IllegalStateException("invalid refresh metadata", exception);
        }
    }

    @Override
    public void revokeToken(String tokenDigest, String reason, Instant now) {
        evaluate(
                List.of(tokenKey(tokenDigest)),
                "REVOKE_TOKEN",
                reason(reason),
                millis(now));
    }

    @Override
    public void revokeSubject(String identitySub, String reason, Instant now) {
        evaluate(
                List.of(subjectIndexKey(identitySub)),
                "REVOKE_SUBJECT",
                reason(reason),
                millis(now));
    }

    @Override
    public void expire(Instant now) {
        // Token keys and subject indexes use PEXPIREAT at creation. Redis performs
        // the global expiry without a KEYS/SCAN pass; this call is intentionally idempotent.
        Objects.requireNonNull(now, "now");
    }

    public String scriptSource() {
        return scriptSource;
    }

    private String evaluate(List<Object> keys, Object... arguments) {
        return script.eval(
                RScript.Mode.READ_WRITE,
                scriptSource,
                RScript.ReturnType.VALUE,
                keys,
                arguments);
    }

    private String tokenKey(String tokenDigest) {
        return keyPrefix + "refresh:" + validDigest(tokenDigest);
    }

    private String subjectIndexKey(String identitySub) {
        return keyPrefix + "refresh-index:user:" + segment(identitySub, "identitySub");
    }

    private static String millis(Instant value) {
        return Long.toString(Objects.requireNonNull(value, "instant").toEpochMilli());
    }

    private static String reason(String value) {
        if (value == null || value.isBlank() || value.length() > 128
                || !value.matches("[A-Z0-9_]+")) {
            throw new IllegalArgumentException("invalid revocation reason");
        }
        return value;
    }

    private static String segment(String value, String field) {
        if (value == null || !SAFE_SEGMENT.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " is not a safe segment");
        }
        return value;
    }

    private static String validDigest(String value) {
        if (value == null || !DIGEST.matcher(value).matches()) {
            throw new IllegalArgumentException("invalid token digest");
        }
        return value;
    }

    private static String validPrefix(String value) {
        if (value == null || value.isBlank() || !value.endsWith(":")
                || value.contains(" ")) {
            throw new IllegalArgumentException("invalid refresh key prefix");
        }
        return value;
    }

    private static String loadScript() {
        try {
            return new ClassPathResource(SCRIPT_LOCATION)
                    .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("cannot load refresh management script", exception);
        }
    }
}
