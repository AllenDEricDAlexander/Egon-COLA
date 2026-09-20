package top.egon.cola.component.accessguard.store;

import top.egon.cola.component.accessguard.common.exception.StoreOperationException;
import java.time.Duration;
import java.util.Optional;

@FunctionalInterface
public interface PenaltyStore {

    Optional<PenaltyState> current(PenaltyKey key);

    default PenaltyState recordViolation(
            PenaltyKey key,
            long threshold,
            Duration violationTtl,
            Duration penaltyTtl
    ) {
        throw new StoreOperationException("PENALTY_WRITE_UNSUPPORTED");
    }

    default int evictExpired() {
        return 0;
    }

    default int size() {
        return 0;
    }
}
