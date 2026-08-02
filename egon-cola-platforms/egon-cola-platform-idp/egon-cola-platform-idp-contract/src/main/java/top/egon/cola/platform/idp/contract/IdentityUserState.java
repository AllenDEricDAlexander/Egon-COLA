package top.egon.cola.platform.idp.contract;

import java.time.Instant;
import java.util.Objects;

public record IdentityUserState(
        String subject,
        Status status,
        long tokenVersion,
        Instant updatedAt
) {

    public IdentityUserState {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("subject is required");
        }
        subject = subject.trim();
        status = Objects.requireNonNull(status, "status");
        if (tokenVersion < 0L) {
            throw new IllegalArgumentException(
                    "tokenVersion must not be negative"
            );
        }
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public enum Status {
        ACTIVE,
        DISABLED,
        LOCKED
    }
}
