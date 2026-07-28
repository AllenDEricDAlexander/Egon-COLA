package top.egon.cola.component.common.id.snowflake;

import java.time.Instant;

/**
 * Parser for the fixed Snowflake layout whose epoch is 2026-01-01T00:00:00Z.
 */
public final class SnowflakeIdParser {

    private SnowflakeIdParser() {
    }

    /**
     * Decodes a non-negative Snowflake ID into its timestamp, machine, and sequence fields.
     *
     * @param id the Snowflake ID to decode
     * @return the decoded ID fields
     * @throws IllegalArgumentException if {@code id} is negative
     */
    public static SnowflakeId parse(long id) {
        if (id < 0L) {
            throw new IllegalArgumentException("Snowflake ID must be non-negative");
        }

        long elapsedMillis = (id >>> SnowflakeIdLayout.ELAPSED_MILLIS_SHIFT)
                & SnowflakeIdLayout.ELAPSED_MILLIS_MASK;
        int machineId = (int) ((id >>> SnowflakeIdLayout.MACHINE_ID_SHIFT)
                & SnowflakeIdLayout.MACHINE_ID_MASK);
        int sequence = (int) (id & SnowflakeIdLayout.SEQUENCE_MASK);
        Instant generatedAt = Instant.ofEpochMilli(SnowflakeIdLayout.EPOCH_MILLIS + elapsedMillis);

        return new SnowflakeId(id, generatedAt, elapsedMillis, machineId, sequence);
    }
}
