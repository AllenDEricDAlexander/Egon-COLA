package top.egon.cola.platform.idp.core.identity;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record IdentityUser(
        String id,
        String username,
        String normalizedUsername,
        String displayName,
        IdentityUserStatus status,
        long tokenVersion,
        int failedLoginCount,
        Instant lockedUntil,
        Instant lastLoginAt,
        long version
) {

    public IdentityUser {
        id = required(id, "id");
        username = required(username, "username");
        normalizedUsername = required(
                normalizedUsername,
                "normalizedUsername"
        );
        displayName = required(displayName, "displayName");
        status = Objects.requireNonNull(status, "status");
        nonNegative(tokenVersion, "tokenVersion");
        nonNegative(failedLoginCount, "failedLoginCount");
        nonNegative(version, "version");
        if (status == IdentityUserStatus.LOCKED && lockedUntil == null) {
            throw new IllegalArgumentException(
                    "lockedUntil is required for a locked user"
            );
        }
    }

    public IdentityUser failedAt(
            Instant now,
            int maximumFailures,
            Duration lockDuration
    ) {
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(lockDuration, "lockDuration");
        if (maximumFailures < 1 || lockDuration.isNegative()
                || lockDuration.isZero()) {
            throw new IllegalArgumentException("invalid lock policy");
        }
        int newFailureCount = Math.addExact(failedLoginCount, 1);
        boolean mustLock = newFailureCount >= maximumFailures;
        return new IdentityUser(
                id,
                username,
                normalizedUsername,
                displayName,
                mustLock ? IdentityUserStatus.LOCKED : status,
                tokenVersion,
                newFailureCount,
                mustLock ? now.plus(lockDuration) : lockedUntil,
                lastLoginAt,
                Math.addExact(version, 1L)
        );
    }

    public IdentityUser unlockIfExpired(Instant now) {
        Objects.requireNonNull(now, "now");
        if (status != IdentityUserStatus.LOCKED
                || lockedUntil == null
                || lockedUntil.isAfter(now)) {
            return this;
        }
        return new IdentityUser(
                id,
                username,
                normalizedUsername,
                displayName,
                IdentityUserStatus.ACTIVE,
                tokenVersion,
                0,
                null,
                lastLoginAt,
                Math.addExact(version, 1L)
        );
    }

    public IdentityUser authenticatedAt(Instant now) {
        return new IdentityUser(
                id,
                username,
                normalizedUsername,
                displayName,
                IdentityUserStatus.ACTIVE,
                tokenVersion,
                0,
                null,
                Objects.requireNonNull(now, "now"),
                Math.addExact(version, 1L)
        );
    }

    public IdentityUser revokeSecurityState() {
        return new IdentityUser(
                id,
                username,
                normalizedUsername,
                displayName,
                status,
                Math.addExact(tokenVersion, 1L),
                failedLoginCount,
                lockedUntil,
                lastLoginAt,
                Math.addExact(version, 1L)
        );
    }

    public IdentityUser withLoginFailure(
            int failureCount,
            Instant lockExpiresAt,
            long newVersion
    ) {
        return new IdentityUser(
                id,
                username,
                normalizedUsername,
                displayName,
                lockExpiresAt == null
                        ? IdentityUserStatus.ACTIVE
                        : IdentityUserStatus.LOCKED,
                tokenVersion,
                failureCount,
                lockExpiresAt,
                lastLoginAt,
                newVersion
        );
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static void nonNegative(long value, String fieldName) {
        if (value < 0L) {
            throw new IllegalArgumentException(
                    fieldName + " must not be negative"
            );
        }
    }
}
