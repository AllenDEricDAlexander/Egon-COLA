package top.egon.cola.platform.idp.admin.token.repo;

import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.core.io.ClassPathResource;
import top.egon.cola.platform.idp.core.port.RefreshTokenStore;
import top.egon.cola.platform.idp.core.token.RefreshFamily;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public final class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final Pattern SAFE_SEGMENT = Pattern.compile(
            "[A-Za-z0-9._~-]{1,128}"
    );
    private static final Pattern DIGEST = Pattern.compile(
            "[A-Za-z0-9_-]{43}"
    );
    private static final String SCRIPT_LOCATION =
            "redis/rotate-refresh-token.lua";

    private final RScript script;
    private final String keyPrefix;
    private final String scriptSource;

    public RedisRefreshTokenStore(
            RedissonClient redisson,
            String keyPrefix
    ) {
        Objects.requireNonNull(redisson, "redisson");
        this.script = redisson.getScript(StringCodec.INSTANCE);
        this.keyPrefix = validPrefix(keyPrefix);
        this.scriptSource = loadScript();
    }

    @Override
    public void create(RefreshFamily family) {
        Objects.requireNonNull(family, "family");
        String result = evaluate(
                List.of(
                        familyKey(family.familyId()),
                        digestKey(family.currentTokenDigest()),
                        subjectIndexKey(family.identitySub())
                ),
                "CREATE",
                family.familyId(),
                family.identitySub(),
                family.tenantId(),
                family.sessionId(),
                family.clientId(),
                Long.toString(family.tokenVersion()),
                Long.toString(family.generation()),
                digest(family.currentTokenDigest()),
                family.status().name(),
                millis(family.createdAt()),
                millis(family.updatedAt()),
                millis(family.expiresAt())
        );
        if (!"CREATED".equals(result)) {
            throw new IllegalStateException("refresh family collision");
        }
    }

    @Override
    public RotationResult rotate(RotationCommand command) {
        Objects.requireNonNull(command, "command");
        String familyId = segment(command.familyId(), "familyId");
        String currentDigest = digest(command.currentTokenDigest());
        String successorDigest = digest(command.successorTokenDigest());
        String result = evaluate(
                List.of(
                        familyKey(familyId),
                        digestKey(currentDigest),
                        digestKey(successorDigest)
                ),
                "ROTATE",
                familyId,
                currentDigest,
                successorDigest,
                Long.toString(command.successorGeneration()),
                segment(command.expectedIdentitySub(), "identitySub"),
                Long.toString(command.expectedTokenVersion()),
                millis(command.expiresAt()),
                millis(command.now())
        );
        try {
            return new RotationResult(
                    RotationOutcome.valueOf(result),
                    null
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "unexpected refresh rotation result",
                    exception
            );
        }
    }

    @Override
    public void revokeFamily(
            String familyId,
            String reason,
            Instant now
    ) {
        evaluate(
                List.of(familyKey(familyId)),
                "REVOKE_FAMILY",
                segment(familyId, "familyId"),
                reason(reason),
                millis(now)
        );
    }

    @Override
    public void revokeSubject(
            String identitySub,
            String reason,
            Instant now
    ) {
        evaluate(
                List.of(subjectIndexKey(identitySub)),
                "REVOKE_SUBJECT",
                reason(reason),
                millis(now)
        );
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
                arguments
        );
    }

    private String familyKey(String familyId) {
        return keyPrefix + "refresh-family:"
                + segment(familyId, "familyId");
    }

    private String digestKey(String tokenDigest) {
        return keyPrefix + "refresh:" + digest(tokenDigest);
    }

    private String subjectIndexKey(String identitySub) {
        return keyPrefix + "refresh-index:user:"
                + segment(identitySub, "identitySub");
    }

    private static String millis(Instant value) {
        return Long.toString(Objects.requireNonNull(value, "instant")
                .toEpochMilli());
    }

    private static String reason(String value) {
        if (value == null
                || value.isBlank()
                || value.length() > 128
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

    private static String digest(String value) {
        if (value == null || !DIGEST.matcher(value).matches()) {
            throw new IllegalArgumentException("invalid token digest");
        }
        return value;
    }

    private static String validPrefix(String value) {
        if (value == null
                || value.isBlank()
                || !value.endsWith(":")
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
            throw new IllegalStateException(
                    "cannot load refresh rotation script",
                    exception
            );
        }
    }
}
