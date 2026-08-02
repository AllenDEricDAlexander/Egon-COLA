package top.egon.cola.platform.idp.core.identity;

import java.time.Instant;
import java.util.Objects;

public record PasswordCredential(
        String identitySub,
        String passwordHash,
        Instant passwordChangedAt,
        boolean mustChangePassword,
        Status status,
        long version
) {

    public PasswordCredential {
        identitySub = required(identitySub, "identitySub");
        passwordHash = required(passwordHash, "passwordHash");
        passwordChangedAt = Objects.requireNonNull(
                passwordChangedAt,
                "passwordChangedAt"
        );
        status = Objects.requireNonNull(status, "status");
        if (version < 0L) {
            throw new IllegalArgumentException(
                    "version must not be negative"
            );
        }
    }

    public PasswordCredential changed(
            String newPasswordHash,
            Instant changedAt
    ) {
        return new PasswordCredential(
                identitySub,
                newPasswordHash,
                changedAt,
                false,
                Status.ACTIVE,
                Math.addExact(version, 1L)
        );
    }

    public PasswordCredential rehashed(String newPasswordHash) {
        return new PasswordCredential(
                identitySub,
                newPasswordHash,
                passwordChangedAt,
                mustChangePassword,
                status,
                Math.addExact(version, 1L)
        );
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    public enum Status {
        ACTIVE,
        REVOKED
    }
}
