package top.egon.cola.component.common.desensitize.logback;

import ch.qos.logback.classic.LoggerContext;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import top.egon.cola.component.common.desensitize.annotation.SensitiveType;
import top.egon.cola.component.common.desensitize.strategy.SensitiveStrategyRegistry;

public final class SensitiveLogs {

    private static final SensitiveStrategyRegistry STRATEGY_REGISTRY =
            SensitiveStrategyRegistry.defaults();

    private SensitiveLogs() {
    }

    public static String of(String value, SensitiveType type) {
        return strategyRegistry().mask(type, value);
    }

    private static SensitiveStrategyRegistry strategyRegistry() {
        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
        if (loggerFactory instanceof LoggerContext loggerContext) {
            Object configured = loggerContext.getObject(
                    SensitiveLogConverter.STRATEGY_REGISTRY_CONTEXT_KEY
            );
            if (configured instanceof SensitiveStrategyRegistry strategyRegistry) {
                return strategyRegistry;
            }
        }
        return STRATEGY_REGISTRY;
    }
}
