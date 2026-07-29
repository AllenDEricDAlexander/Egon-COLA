package top.egon.cola.component.accessguard.store;

import java.time.Instant;

public record PenaltyState(
        long violations,
        boolean active,
        Instant violationExpiresAt,
        Instant penaltyExpiresAt
) {

    public PenaltyState {
        if (violations < 0) {
            throw new IllegalArgumentException("violations must not be negative");
        }
    }
}
