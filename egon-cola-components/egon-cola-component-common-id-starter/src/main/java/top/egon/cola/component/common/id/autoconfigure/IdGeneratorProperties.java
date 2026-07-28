package top.egon.cola.component.common.id.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;

import java.time.Duration;

/**
 * Configuration properties for Snowflake ID generation. Machine identifiers
 * are deployment-assigned and are never derived automatically.
 */
@ConfigurationProperties(prefix = IdGeneratorProperties.PREFIX)
public class IdGeneratorProperties {

    /** Configuration prefix for the common ID Starter. */
    public static final String PREFIX = "egon.cola.component.id";

    private boolean enabled = true;
    private Long machineId;
    private Duration maxClockBackward = SnowflakeIdGenerator.DEFAULT_MAX_CLOCK_BACKWARD;

    /**
     * Returns whether Snowflake auto-configuration is enabled.
     *
     * @return {@code true} when enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables or disables Snowflake auto-configuration.
     *
     * @param enabled whether generation is enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns the explicitly assigned machine identifier.
     *
     * @return machine identifier from 0 to 1023, or {@code null} when absent
     */
    public Long getMachineId() {
        return machineId;
    }

    /**
     * Sets the explicitly assigned machine identifier.
     *
     * @param machineId machine identifier from 0 to 1023
     */
    public void setMachineId(Long machineId) {
        this.machineId = machineId;
    }

    /**
     * Returns the largest clock rollback that may be waited out.
     *
     * @return non-negative rollback tolerance
     */
    public Duration getMaxClockBackward() {
        return maxClockBackward;
    }

    /**
     * Sets the largest clock rollback that may be waited out.
     *
     * @param maxClockBackward non-negative rollback tolerance
     */
    public void setMaxClockBackward(Duration maxClockBackward) {
        this.maxClockBackward = maxClockBackward;
    }
}
