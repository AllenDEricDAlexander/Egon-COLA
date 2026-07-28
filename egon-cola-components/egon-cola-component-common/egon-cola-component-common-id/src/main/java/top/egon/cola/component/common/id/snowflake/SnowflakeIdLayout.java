package top.egon.cola.component.common.id.snowflake;

/**
 * Shared fixed layout for Snowflake IDs with a 2026-01-01T00:00:00Z epoch.
 */
final class SnowflakeIdLayout {

    static final long EPOCH_MILLIS = 1_767_225_600_000L;

    static final int ELAPSED_MILLIS_BITS = 41;
    static final int MACHINE_ID_BITS = 10;
    static final int SEQUENCE_BITS = 12;

    static final int MACHINE_ID_SHIFT = SEQUENCE_BITS;
    static final int ELAPSED_MILLIS_SHIFT = MACHINE_ID_BITS + SEQUENCE_BITS;

    static final long ELAPSED_MILLIS_MASK = (1L << ELAPSED_MILLIS_BITS) - 1L;
    static final long MACHINE_ID_MASK = (1L << MACHINE_ID_BITS) - 1L;
    static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) - 1L;

    static final long MAX_ELAPSED_MILLIS = ELAPSED_MILLIS_MASK;
    static final long MAX_TIMESTAMP_MILLIS = EPOCH_MILLIS + MAX_ELAPSED_MILLIS;
    static final int MAX_MACHINE_ID = (int) MACHINE_ID_MASK;
    static final int MAX_SEQUENCE = (int) SEQUENCE_MASK;

    private SnowflakeIdLayout() {
    }

    static long compose(long elapsedMillis, int machineId, int sequence) {
        return (elapsedMillis << ELAPSED_MILLIS_SHIFT)
                | ((long) machineId << MACHINE_ID_SHIFT)
                | sequence;
    }

    static long packState(long elapsedMillis, int sequence) {
        return (elapsedMillis << SEQUENCE_BITS) | sequence;
    }

    static long stateElapsedMillis(long state) {
        return state >>> SEQUENCE_BITS;
    }

    static int stateSequence(long state) {
        return (int) (state & SEQUENCE_MASK);
    }
}
