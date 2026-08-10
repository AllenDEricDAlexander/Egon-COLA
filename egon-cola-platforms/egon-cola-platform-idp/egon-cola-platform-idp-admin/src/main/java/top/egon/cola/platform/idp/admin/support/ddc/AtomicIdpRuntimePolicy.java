package top.egon.cola.platform.idp.admin.support.ddc;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public final class AtomicIdpRuntimePolicy implements IdpRuntimePolicy {

    public static final String ACCESS_TOKEN_TTL_KEY =
            "idp.token.access-ttl";
    public static final String REFRESH_TOKEN_TTL_KEY =
            "idp.token.refresh-ttl";
    public static final String AUTHORIZATION_CODE_TTL_KEY =
            "idp.authorization-code.ttl";
    public static final String MAXIMUM_LOGIN_FAILURES_KEY =
            "idp.login.max-failures";
    public static final String LOGIN_LOCK_DURATION_KEY =
            "idp.login.lock-duration";
    public static final String PASSWORD_MAXIMUM_CONCURRENCY_KEY =
            "idp.password.max-concurrency";

    public static final Set<String> CONFIG_KEYS = Set.of(
            ACCESS_TOKEN_TTL_KEY,
            REFRESH_TOKEN_TTL_KEY,
            AUTHORIZATION_CODE_TTL_KEY,
            MAXIMUM_LOGIN_FAILURES_KEY,
            LOGIN_LOCK_DURATION_KEY,
            PASSWORD_MAXIMUM_CONCURRENCY_KEY
    );

    private static final Pattern UNSIGNED_INTEGER = Pattern.compile("[0-9]+");

    private final AtomicReference<Snapshot> snapshot = new AtomicReference<>(
            defaults()
    );

    @Override
    public Snapshot current() {
        return snapshot.get();
    }

    public synchronized void apply(String key, String rawValue, long version) {
        try {
            requireKnownKey(key);
            long value = parse(rawValue);
            Snapshot current = snapshot.get();
            long currentVersion = current.configVersions().get(key);
            if (version < currentVersion) {
                throw new PolicyApplyException("STALE_CONFIG_VERSION");
            }
            snapshot.set(candidate(current, key, value, version));
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException(
                    "IdP runtime policy rejected key=" + safeKey(key)
                            + " version=" + version
                            + " code=" + errorCode(failure),
                    failure
            );
        }
    }

    private Snapshot candidate(
            Snapshot current,
            String key,
            long value,
            long version
    ) {
        Duration accessTtl = current.accessTokenTtl();
        Duration refreshTtl = current.refreshTokenTtl();
        Duration codeTtl = current.authorizationCodeTtl();
        int failures = current.maximumLoginFailures();
        Duration lockDuration = current.loginLockDuration();
        int concurrency = current.passwordMaximumConcurrency();
        switch (key) {
            case ACCESS_TOKEN_TTL_KEY -> accessTtl = Duration.ofSeconds(value);
            case REFRESH_TOKEN_TTL_KEY -> refreshTtl = Duration.ofSeconds(value);
            case AUTHORIZATION_CODE_TTL_KEY -> codeTtl =
                    Duration.ofSeconds(value);
            case MAXIMUM_LOGIN_FAILURES_KEY -> failures = integer(value);
            case LOGIN_LOCK_DURATION_KEY -> lockDuration =
                    Duration.ofSeconds(value);
            case PASSWORD_MAXIMUM_CONCURRENCY_KEY -> concurrency =
                    integer(value);
            default -> throw new PolicyApplyException("UNKNOWN_KEY");
        }
        Map<String, Long> versions = new LinkedHashMap<>(
                current.configVersions()
        );
        versions.put(key, version);
        return new Snapshot(
                accessTtl,
                refreshTtl,
                codeTtl,
                failures,
                lockDuration,
                concurrency,
                versions
        );
    }

    private long parse(String rawValue) {
        if (rawValue == null || !UNSIGNED_INTEGER.matcher(rawValue).matches()) {
            throw new PolicyApplyException("INVALID_INTEGER");
        }
        try {
            return Long.parseLong(rawValue);
        } catch (NumberFormatException exception) {
            throw new PolicyApplyException("INVALID_INTEGER", exception);
        }
    }

    private int integer(long value) {
        try {
            return Math.toIntExact(value);
        } catch (ArithmeticException exception) {
            throw new PolicyApplyException("INTEGER_OUT_OF_RANGE", exception);
        }
    }

    private void requireKnownKey(String key) {
        if (!CONFIG_KEYS.contains(key)) {
            throw new PolicyApplyException("UNKNOWN_KEY");
        }
    }

    private String errorCode(RuntimeException failure) {
        if (failure instanceof PolicyApplyException policyFailure) {
            return policyFailure.errorCode;
        }
        String message = failure.getMessage();
        return message == null || !message.matches("[A-Z0-9_]+")
                ? "INVALID_POLICY"
                : message;
    }

    private String safeKey(String key) {
        return key == null || key.isBlank() ? "<missing>" : key;
    }

    private static Snapshot defaults() {
        Map<String, Long> versions = new LinkedHashMap<>();
        CONFIG_KEYS.stream().sorted().forEach(key -> versions.put(key, 0L));
        return new Snapshot(
                Duration.ofMinutes(15),
                Duration.ofDays(7),
                Duration.ofSeconds(60),
                5,
                Duration.ofMinutes(15),
                8,
                versions
        );
    }

    private static final class PolicyApplyException
            extends IllegalArgumentException {

        private final String errorCode;

        private PolicyApplyException(String errorCode) {
            super(errorCode);
            this.errorCode = errorCode;
        }

        private PolicyApplyException(String errorCode, Throwable cause) {
            super(errorCode, cause);
            this.errorCode = errorCode;
        }
    }
}
