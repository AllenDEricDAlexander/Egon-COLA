package top.egon.cola.platform.rbac3.admin.integration.ddc;

import top.egon.cola.platform.rbac3.admin.application.port.Rbac3RuntimePolicy;
import top.egon.cola.platform.rbac3.admin.config.Rbac3AdminProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * Applies validated scalar configuration by atomically replacing a complete snapshot.
 */
public final class AtomicRbac3RuntimePolicy implements Rbac3RuntimePolicy {

    public static final String ACCESS_TOKEN_TTL_KEY =
            "rbac3.access-token-ttl-seconds";
    public static final String REFRESH_TOKEN_TTL_KEY =
            "rbac3.refresh-token-ttl-seconds";
    public static final String SESSION_IDLE_TIMEOUT_KEY =
            "rbac3.session-idle-timeout-seconds";
    public static final String SESSION_ABSOLUTE_TIMEOUT_KEY =
            "rbac3.session-absolute-timeout-seconds";
    public static final String MAXIMUM_ACTIVE_ROOTS_KEY =
            "rbac3.maximum-active-roots";

    public static final Set<String> CONFIG_KEYS = Set.of(
            ACCESS_TOKEN_TTL_KEY,
            REFRESH_TOKEN_TTL_KEY,
            SESSION_IDLE_TIMEOUT_KEY,
            SESSION_ABSOLUTE_TIMEOUT_KEY,
            MAXIMUM_ACTIVE_ROOTS_KEY);

    private static final Pattern UNSIGNED_INTEGER = Pattern.compile("[0-9]+");
    private static final Set<String> POLICY_ERROR_CODES = Set.of(
            "ACCESS_TOKEN_TTL_OUT_OF_RANGE",
            "REFRESH_TOKEN_TTL_OUT_OF_RANGE",
            "SESSION_IDLE_TIMEOUT_OUT_OF_RANGE",
            "SESSION_ABSOLUTE_TIMEOUT_OUT_OF_RANGE",
            "MAXIMUM_ACTIVE_ROOTS_OUT_OF_RANGE",
            "IDLE_EXCEEDS_ABSOLUTE",
            "REFRESH_BELOW_ABSOLUTE",
            "INVALID_CONFIG_VERSION");

    private final AtomicReference<Snapshot> snapshot;
    private final AtomicReference<ApplyFailure> lastApplyFailure = new AtomicReference<>();

    public AtomicRbac3RuntimePolicy(Rbac3AdminProperties properties) {
        Objects.requireNonNull(properties, "properties");
        Map<String, Long> versions = new LinkedHashMap<>();
        CONFIG_KEYS.stream().sorted().forEach(key -> versions.put(key, 0L));
        snapshot = new AtomicReference<>(new Snapshot(
                properties.getAccessTokenTtl(),
                properties.getRefreshTokenTtl(),
                properties.getSessionIdleTimeout(),
                properties.getSessionAbsoluteTimeout(),
                properties.getMaximumActiveRoots(),
                versions));
    }

    @Override
    public Snapshot current() {
        return snapshot.get();
    }

    public Optional<ApplyFailure> lastApplyFailure() {
        return Optional.ofNullable(lastApplyFailure.get());
    }

    public synchronized void apply(String key, String rawValue, long version) {
        try {
            requireKnownKey(key);
            if (version < 0) {
                throw new PolicyApplyException("INVALID_VERSION");
            }
            long parsed = parse(rawValue);
            Snapshot candidate = candidate(snapshot.get(), key, parsed, version);
            snapshot.set(candidate);
            ApplyFailure previousFailure = lastApplyFailure.get();
            if (previousFailure != null && previousFailure.key().equals(key)) {
                lastApplyFailure.compareAndSet(previousFailure, null);
            }
        } catch (RuntimeException failure) {
            String errorCode = errorCode(failure);
            lastApplyFailure.set(new ApplyFailure(safeKey(key), version, errorCode));
            throw new IllegalArgumentException(
                    "RBAC3 runtime policy rejected key=" + safeKey(key)
                            + " version=" + version + " code=" + errorCode,
                    failure);
        }
    }

    private Snapshot candidate(Snapshot current, String key, long value, long version) {
        Duration accessTokenTtl = current.accessTokenTtl();
        Duration refreshTokenTtl = current.refreshTokenTtl();
        Duration sessionIdleTimeout = current.sessionIdleTimeout();
        Duration sessionAbsoluteTimeout = current.sessionAbsoluteTimeout();
        int maximumActiveRoots = current.maximumActiveRoots();
        switch (key) {
            case ACCESS_TOKEN_TTL_KEY -> accessTokenTtl = Duration.ofSeconds(value);
            case REFRESH_TOKEN_TTL_KEY -> refreshTokenTtl = Duration.ofSeconds(value);
            case SESSION_IDLE_TIMEOUT_KEY -> sessionIdleTimeout = Duration.ofSeconds(value);
            case SESSION_ABSOLUTE_TIMEOUT_KEY ->
                    sessionAbsoluteTimeout = Duration.ofSeconds(value);
            case MAXIMUM_ACTIVE_ROOTS_KEY -> maximumActiveRoots = integer(value);
            default -> throw new PolicyApplyException("UNKNOWN_KEY");
        }
        Map<String, Long> versions = new LinkedHashMap<>(current.configVersions());
        versions.put(key, version);
        return new Snapshot(
                accessTokenTtl,
                refreshTokenTtl,
                sessionIdleTimeout,
                sessionAbsoluteTimeout,
                maximumActiveRoots,
                versions);
    }

    private void requireKnownKey(String key) {
        if (key == null || !CONFIG_KEYS.contains(key)) {
            throw new PolicyApplyException("UNKNOWN_KEY");
        }
    }

    private long parse(String rawValue) {
        if (rawValue == null || !UNSIGNED_INTEGER.matcher(rawValue).matches()) {
            throw new PolicyApplyException("INVALID_INTEGER");
        }
        try {
            return Long.parseLong(rawValue);
        } catch (NumberFormatException invalid) {
            throw new PolicyApplyException("INVALID_INTEGER", invalid);
        }
    }

    private int integer(long value) {
        try {
            return Math.toIntExact(value);
        } catch (ArithmeticException overflow) {
            throw new PolicyApplyException("MAXIMUM_ACTIVE_ROOTS_OUT_OF_RANGE", overflow);
        }
    }

    private String errorCode(RuntimeException failure) {
        if (failure instanceof PolicyApplyException policyFailure) {
            return policyFailure.errorCode;
        }
        String message = failure.getMessage();
        return POLICY_ERROR_CODES.contains(message) ? message : "INVALID_POLICY";
    }

    private String safeKey(String key) {
        return key == null || key.isBlank() ? "<missing>" : key;
    }

    public record ApplyFailure(String key, long targetVersion, String errorCode) {

        public ApplyFailure {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(errorCode, "errorCode");
        }
    }

    private static final class PolicyApplyException extends IllegalArgumentException {

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
