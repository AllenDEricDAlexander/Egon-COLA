package top.egon.cola.component.common.desensitize.logback;

import ch.qos.logback.classic.LoggerContext;
import top.egon.cola.component.common.desensitize.strategy.SensitiveStrategyRegistry;

import java.util.ArrayList;
import java.util.List;

public final class SensitiveLogbackRegistryBridge implements AutoCloseable {

    private static final String REGISTRATIONS_CONTEXT_KEY =
            "top.egon.cola.component.common.desensitize.strategyRegistryRegistrations";

    private final LoggerContext loggerContext;

    private final Object registrationToken = new Object();

    private volatile boolean closed;

    public SensitiveLogbackRegistryBridge(
            LoggerContext loggerContext,
            SensitiveStrategyRegistry strategyRegistry) {
        this.loggerContext = loggerContext;
        if (loggerContext != null) {
            synchronized (loggerContext) {
                List<Registration> registrations = registrations(loggerContext);
                registrations.add(new Registration(registrationToken, strategyRegistry));
                updateContext(loggerContext, registrations);
            }
        }
    }

    @Override
    public void close() {
        if (loggerContext == null || closed) {
            return;
        }
        synchronized (loggerContext) {
            if (closed) {
                return;
            }
            List<Registration> registrations = registrations(loggerContext);
            registrations.removeIf(registration ->
                    registration.token() == registrationToken);
            updateContext(loggerContext, registrations);
            closed = true;
        }
    }

    private List<Registration> registrations(LoggerContext context) {
        Object value = context.getObject(REGISTRATIONS_CONTEXT_KEY);
        if (!(value instanceof List<?> registrations)) {
            return new ArrayList<>();
        }
        List<Registration> copy = new ArrayList<>(registrations.size());
        for (Object registration : registrations) {
            if (registration instanceof Registration typedRegistration) {
                copy.add(typedRegistration);
            }
        }
        return copy;
    }

    private void updateContext(LoggerContext context,
                               List<Registration> registrations) {
        if (registrations.isEmpty()) {
            context.removeObject(REGISTRATIONS_CONTEXT_KEY);
            context.removeObject(SensitiveLogConverter.STRATEGY_REGISTRY_CONTEXT_KEY);
            return;
        }
        context.putObject(REGISTRATIONS_CONTEXT_KEY, List.copyOf(registrations));
        context.putObject(
                SensitiveLogConverter.STRATEGY_REGISTRY_CONTEXT_KEY,
                registrations.getLast().strategyRegistry()
        );
    }

    private record Registration(
            Object token,
            SensitiveStrategyRegistry strategyRegistry) {
    }
}
