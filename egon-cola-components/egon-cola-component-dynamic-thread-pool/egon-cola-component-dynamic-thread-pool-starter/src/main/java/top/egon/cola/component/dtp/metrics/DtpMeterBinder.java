package top.egon.cola.component.dtp.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot;
import top.egon.cola.component.dtp.executor.ManagedExecutor;
import top.egon.cola.component.dtp.executor.ManagedExecutorRegistry;

import java.util.function.Function;

/**
 * @ClassName: DtpMeterBinder
 * @description: 动态线程池 Micrometer 指标绑定器
 * @author: 有罗敷的马同学
 * @datetime: 2026Year-06Month-30Day
 * @Version: 1.0
 */
public class DtpMeterBinder implements MeterBinder {

    private final ManagedExecutorRegistry managedExecutorRegistry;

    public DtpMeterBinder(ManagedExecutorRegistry managedExecutorRegistry) {
        this.managedExecutorRegistry = managedExecutorRegistry;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        if (managedExecutorRegistry == null) {
            return;
        }
        for (ManagedExecutor managedExecutor : managedExecutorRegistry.list()) {
            if (managedExecutor == null) {
                continue;
            }
            ExecutorSnapshot snapshot = managedExecutor.snapshot();
            Tags tags = tags(snapshot);
            registerCommonMeters(registry, managedExecutor, tags);
            if (snapshot != null && snapshot.isVirtual()) {
                registerVirtualMeters(registry, managedExecutor, tags);
            } else {
                registerPlatformMeters(registry, managedExecutor, tags);
            }
        }
    }

    private void registerCommonMeters(MeterRegistry registry, ManagedExecutor managedExecutor, Tags tags) {
        gauge(registry, "dtp.executor.completed", managedExecutor, ExecutorSnapshot::getCompletedTaskCount, tags);
        gauge(registry, "dtp.executor.rejected", managedExecutor, ExecutorSnapshot::getRejectedTasks, tags);
    }

    private void registerPlatformMeters(MeterRegistry registry, ManagedExecutor managedExecutor, Tags tags) {
        gauge(registry, "dtp.executor.active", managedExecutor, ExecutorSnapshot::getActiveCount, tags);
        gauge(registry, "dtp.executor.pool.size", managedExecutor, ExecutorSnapshot::getPoolSize, tags);
        gauge(registry, "dtp.executor.pool.core", managedExecutor, ExecutorSnapshot::getCorePoolSize, tags);
        gauge(registry, "dtp.executor.pool.max", managedExecutor, ExecutorSnapshot::getMaximumPoolSize, tags);
        gauge(registry, "dtp.executor.queue.size", managedExecutor, ExecutorSnapshot::getQueueSize, tags);
        gauge(registry, "dtp.executor.queue.remaining", managedExecutor, ExecutorSnapshot::getRemainingCapacity, tags);
    }

    private void registerVirtualMeters(MeterRegistry registry, ManagedExecutor managedExecutor, Tags tags) {
        gauge(registry, "dtp.executor.virtual.running", managedExecutor, ExecutorSnapshot::getRunningTasks, tags);
        gauge(registry, "dtp.executor.virtual.submitted", managedExecutor, ExecutorSnapshot::getSubmittedTasks, tags);
        gauge(registry, "dtp.executor.virtual.completed", managedExecutor, ExecutorSnapshot::getCompletedTaskCount, tags);
        gauge(registry, "dtp.executor.virtual.failed", managedExecutor, ExecutorSnapshot::getFailedTasks, tags);
        gauge(registry, "dtp.executor.virtual.permits.available", managedExecutor, ExecutorSnapshot::getAvailablePermits, tags);
    }

    private void gauge(MeterRegistry registry, String name, ManagedExecutor managedExecutor,
                       Function<ExecutorSnapshot, Number> valueFunction, Tags tags) {
        Gauge.builder(name, managedExecutor, executor -> value(executor, valueFunction))
                .tags(tags)
                .register(registry);
    }

    private Tags tags(ExecutorSnapshot snapshot) {
        return Tags.of(
                "appName", tagValue(snapshot == null ? null : snapshot.getAppName()),
                "instanceId", tagValue(snapshot == null ? null : snapshot.getInstanceId()),
                "executorName", tagValue(snapshot == null ? null : snapshot.getExecutorName()),
                "executorKind", tagValue(snapshot == null || snapshot.getExecutorKind() == null ? null : snapshot.getExecutorKind().name()),
                "virtual", String.valueOf(snapshot != null && snapshot.isVirtual())
        );
    }

    private String tagValue(String value) {
        return value == null ? "" : value;
    }

    private double value(ManagedExecutor managedExecutor, Function<ExecutorSnapshot, Number> valueFunction) {
        ExecutorSnapshot snapshot = managedExecutor.snapshot();
        if (snapshot == null) {
            return 0D;
        }
        return value(valueFunction.apply(snapshot));
    }

    private double value(Number number) {
        return number == null ? 0D : number.doubleValue();
    }

}
