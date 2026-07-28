package top.egon.cola.component.common.id.exception;

/**
 * Signals that a thread was interrupted while waiting for a safe Snowflake
 * timestamp. The interrupted status remains set when this exception is thrown.
 */
public final class IdGenerationInterruptedException extends IllegalStateException {

    /**
     * Creates an interrupted generation failure.
     *
     * @param machineId generator machine identifier
     */
    public IdGenerationInterruptedException(int machineId) {
        super("Interrupted while waiting to generate Snowflake ID: machineId=" + machineId);
    }
}
