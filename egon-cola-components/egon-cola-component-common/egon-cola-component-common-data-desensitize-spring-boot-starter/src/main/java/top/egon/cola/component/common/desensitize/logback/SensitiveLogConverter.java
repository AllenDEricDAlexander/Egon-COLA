package top.egon.cola.component.common.desensitize.logback;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.helpers.MessageFormatter;
import top.egon.cola.component.common.desensitize.metadata.SensitiveMetadataResolver;
import top.egon.cola.component.common.desensitize.strategy.SensitiveStrategyRegistry;

/**
 * Logback message converter for {@code %sensitiveMsg}. The conversion word must replace
 * {@code %msg} in the application pattern so that the unmasked formatted message is not emitted.
 */
public class SensitiveLogConverter extends ClassicConverter {

    public static final String STRATEGY_REGISTRY_CONTEXT_KEY =
            "top.egon.cola.component.common.desensitize.strategyRegistry";

    private final SensitiveStrategyRegistry fallbackStrategyRegistry;

    private final SensitiveLogArgumentFormatter argumentFormatter;

    public SensitiveLogConverter() {
        this(SensitiveStrategyRegistry.defaults(), new SensitiveMetadataResolver());
    }

    protected SensitiveLogConverter(SensitiveStrategyRegistry strategyRegistry,
                                    SensitiveMetadataResolver metadataResolver) {
        this.fallbackStrategyRegistry = strategyRegistry;
        this.argumentFormatter = new SensitiveLogArgumentFormatter(metadataResolver);
    }

    @Override
    public String convert(ILoggingEvent event) {
        String message = event.getMessage();
        Object[] arguments = event.getArgumentArray();
        if (arguments == null || arguments.length == 0) {
            return message;
        }
        return MessageFormatter.arrayFormat(
                message,
                argumentFormatter.sanitize(arguments, strategyRegistry())
        ).getMessage();
    }

    private SensitiveStrategyRegistry strategyRegistry() {
        if (getContext() == null) {
            return fallbackStrategyRegistry;
        }
        Object configured = getContext().getObject(STRATEGY_REGISTRY_CONTEXT_KEY);
        return configured instanceof SensitiveStrategyRegistry strategyRegistry
                ? strategyRegistry
                : fallbackStrategyRegistry;
    }
}
