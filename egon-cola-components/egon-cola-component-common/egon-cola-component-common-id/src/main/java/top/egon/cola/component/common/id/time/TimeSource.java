package top.egon.cola.component.common.id.time;

/**
 * Source of wall-clock time for ID generation.
 */
@FunctionalInterface
public interface TimeSource {

    /**
     * Returns the current wall-clock time in milliseconds from the Unix epoch.
     *
     * @return the current time in milliseconds
     */
    long currentTimeMillis();
}
