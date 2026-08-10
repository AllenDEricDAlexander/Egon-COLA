package top.egon.cola.platform.idp.admin.support.ddc;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

@FunctionalInterface
public interface IdpRuntimePolicy {

    Snapshot current();

    record Snapshot(
            Duration accessTokenTtl,
            Duration refreshTokenTtl,
            Duration authorizationCodeTtl,
            int maximumLoginFailures,
            Duration loginLockDuration,
            int passwordMaximumConcurrency,
            Map<String, Long> configVersions
    ) {

        public Snapshot {
            accessTokenTtl = durationInRange(
                    accessTokenTtl,
                    Duration.ofMinutes(5),
                    Duration.ofMinutes(30),
                    "ACCESS_TOKEN_TTL_OUT_OF_RANGE"
            );
            refreshTokenTtl = durationInRange(
                    refreshTokenTtl,
                    Duration.ofDays(1),
                    Duration.ofDays(30),
                    "REFRESH_TOKEN_TTL_OUT_OF_RANGE"
            );
            authorizationCodeTtl = durationInRange(
                    authorizationCodeTtl,
                    Duration.ofSeconds(30),
                    Duration.ofMinutes(5),
                    "AUTHORIZATION_CODE_TTL_OUT_OF_RANGE"
            );
            loginLockDuration = durationInRange(
                    loginLockDuration,
                    Duration.ofMinutes(1),
                    Duration.ofHours(1),
                    "LOGIN_LOCK_DURATION_OUT_OF_RANGE"
            );
            if (maximumLoginFailures < 3 || maximumLoginFailures > 20) {
                throw new IllegalArgumentException(
                        "MAXIMUM_LOGIN_FAILURES_OUT_OF_RANGE"
                );
            }
            if (passwordMaximumConcurrency < 1
                    || passwordMaximumConcurrency > 64) {
                throw new IllegalArgumentException(
                        "PASSWORD_MAXIMUM_CONCURRENCY_OUT_OF_RANGE"
                );
            }
            configVersions = Map.copyOf(Objects.requireNonNull(
                    configVersions,
                    "configVersions"
            ));
            configVersions.forEach((key, version) -> {
                if (key == null
                        || key.isBlank()
                        || version == null
                        || version < 0L) {
                    throw new IllegalArgumentException(
                            "INVALID_CONFIG_VERSION"
                    );
                }
            });
        }

        private static Duration durationInRange(
                Duration value,
                Duration minimum,
                Duration maximum,
                String errorCode
        ) {
            Objects.requireNonNull(value, errorCode);
            if (value.compareTo(minimum) < 0
                    || value.compareTo(maximum) > 0) {
                throw new IllegalArgumentException(errorCode);
            }
            return value;
        }
    }
}
