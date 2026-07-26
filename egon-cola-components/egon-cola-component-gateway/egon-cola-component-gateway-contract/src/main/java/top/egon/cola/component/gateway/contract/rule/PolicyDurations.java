package top.egon.cola.component.gateway.contract.rule;

import java.time.Duration;
import java.util.Objects;

/**
 * Duration validation shared by the nested policy records.
 *
 * <p>This lives in its own class rather than as a private static method on
 * {@link ServiceCallPolicy} to keep the nested records independent of the outer class's
 * initialisation. When they called an outer helper, initialising a nested record first made
 * the JVM start {@code ServiceCallPolicy.<clinit>}, which re-entered the nested record's own
 * still-running initialiser, read its {@code DEFAULTS} as null, and permanently cached a
 * {@code ServiceCallPolicy} with null components. Keeping the helper here breaks that cycle.
 */
final class PolicyDurations {

    private PolicyDurations() {
    }

    static Duration requirePositive(Duration value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isNegative() || value.isZero()) {
            throw new IllegalArgumentException(field + " must be positive but was " + value);
        }
        return value;
    }
}
