package top.egon.cola.platform.rbac3.admin.application.port;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Supplies one complete runtime-policy snapshot to each RBAC3 command.
 */
@FunctionalInterface
public interface Rbac3RuntimePolicy {

    Snapshot current();

    record Snapshot(
            Duration accessTokenTtl,
            Duration refreshTokenTtl,
            Duration sessionIdleTimeout,
            Duration sessionAbsoluteTimeout,
            int maximumActiveRoots,
            Map<String, Long> configVersions
    ) {

        private static final Duration MIN_ACCESS_TOKEN_TTL = Duration.ofMinutes(5);
        private static final Duration MAX_ACCESS_TOKEN_TTL = Duration.ofMinutes(30);
        private static final Duration MIN_REFRESH_TOKEN_TTL = Duration.ofDays(1);
        private static final Duration MAX_REFRESH_TOKEN_TTL = Duration.ofDays(30);
        private static final Duration MIN_IDLE_TIMEOUT = Duration.ofMinutes(5);
        private static final Duration MAX_IDLE_TIMEOUT = Duration.ofHours(8);
        private static final Duration MIN_ABSOLUTE_TIMEOUT = Duration.ofHours(1);
        private static final Duration MAX_ABSOLUTE_TIMEOUT = Duration.ofHours(24);

        public Snapshot {
            accessTokenTtl = Objects.requireNonNull(accessTokenTtl, "accessTokenTtl");
            refreshTokenTtl = Objects.requireNonNull(refreshTokenTtl, "refreshTokenTtl");
            sessionIdleTimeout = Objects.requireNonNull(
                    sessionIdleTimeout, "sessionIdleTimeout");
            sessionAbsoluteTimeout = Objects.requireNonNull(
                    sessionAbsoluteTimeout, "sessionAbsoluteTimeout");
            configVersions = Map.copyOf(Objects.requireNonNull(
                    configVersions, "configVersions"));
            requireRange(accessTokenTtl, MIN_ACCESS_TOKEN_TTL, MAX_ACCESS_TOKEN_TTL,
                    "ACCESS_TOKEN_TTL_OUT_OF_RANGE");
            requireRange(sessionIdleTimeout, MIN_IDLE_TIMEOUT, MAX_IDLE_TIMEOUT,
                    "SESSION_IDLE_TIMEOUT_OUT_OF_RANGE");
            requireRange(sessionAbsoluteTimeout, MIN_ABSOLUTE_TIMEOUT, MAX_ABSOLUTE_TIMEOUT,
                    "SESSION_ABSOLUTE_TIMEOUT_OUT_OF_RANGE");
            if (maximumActiveRoots < 1 || maximumActiveRoots > 32) {
                throw new IllegalArgumentException("MAXIMUM_ACTIVE_ROOTS_OUT_OF_RANGE");
            }
            if (sessionIdleTimeout.compareTo(sessionAbsoluteTimeout) > 0) {
                throw new IllegalArgumentException("IDLE_EXCEEDS_ABSOLUTE");
            }
            if (refreshTokenTtl.compareTo(sessionAbsoluteTimeout) < 0) {
                throw new IllegalArgumentException("REFRESH_BELOW_ABSOLUTE");
            }
            requireRange(refreshTokenTtl, MIN_REFRESH_TOKEN_TTL, MAX_REFRESH_TOKEN_TTL,
                    "REFRESH_TOKEN_TTL_OUT_OF_RANGE");
            configVersions.forEach((key, version) -> {
                if (key == null || key.isBlank() || version == null || version < 0) {
                    throw new IllegalArgumentException("INVALID_CONFIG_VERSION");
                }
            });
        }

        private static void requireRange(
                Duration value,
                Duration minimum,
                Duration maximum,
                String errorCode) {
            if (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
                throw new IllegalArgumentException(errorCode);
            }
        }
    }
}
