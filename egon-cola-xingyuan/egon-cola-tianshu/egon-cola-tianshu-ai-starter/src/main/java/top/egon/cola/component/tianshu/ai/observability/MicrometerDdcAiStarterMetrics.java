package top.egon.cola.component.tianshu.ai.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Objects;

/**
 * 延迟解析 MeterRegistry 的计数器实现。 / Counter implementation that resolves the MeterRegistry lazily.
 */
public class MicrometerDdcAiStarterMetrics implements DdcAiStarterMetrics {

    private final ObjectProvider<MeterRegistry> registry;

    public MicrometerDdcAiStarterMetrics(ObjectProvider<MeterRegistry> registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public void increment(String name, long amount, String... tags) {
        MeterRegistry meters = registry.getIfAvailable();
        if (meters != null && amount > 0) meters.counter(name, tags).increment(amount);
    }
}
