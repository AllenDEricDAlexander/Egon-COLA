package top.egon.cola.component.common.log;

import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.util.Objects;

/**
 * Entry point for controlled structured business logs.
 */
public final class BizLog {

    private BizLog() {
    }

    public static BizLogBuilder debug(Logger logger) {
        return builder(logger, Level.DEBUG);
    }

    public static BizLogBuilder info(Logger logger) {
        return builder(logger, Level.INFO);
    }

    public static BizLogBuilder warn(Logger logger) {
        return builder(logger, Level.WARN);
    }

    public static BizLogBuilder error(Logger logger) {
        return builder(logger, Level.ERROR);
    }

    private static BizLogBuilder builder(Logger logger, Level level) {
        return new BizLogBuilder(Objects.requireNonNull(logger, "logger"), level);
    }
}
