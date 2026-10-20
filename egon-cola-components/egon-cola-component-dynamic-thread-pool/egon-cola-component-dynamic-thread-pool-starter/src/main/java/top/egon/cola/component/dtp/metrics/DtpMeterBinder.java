package top.egon.cola.component.dtp.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.DisposableBean;
import top.egon.cola.component.dtp.context.DtpTaskObserver;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
@Slf4j
public class DtpMeterBinder implements MeterBinder, DisposableBean {

    private final ManagedExecutorRegistry managedExecutorRegistry;
    private final Map<MeterRegistry, Binding> bindings = new ConcurrentHashMap<>();
    private final ThreadLocal<List<Meter>> adding = new ThreadLocal<>();

    public DtpMeterBinder(ManagedExecutorRegistry managedExecutorRegistry) {
        this.managedExecutorRegistry = managedExecutorRegistry;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        if (managedExecutorRegistry == null) { return; }
        Binding binding = new Binding(registry);
        if (bindings.putIfAbsent(registry, binding) != null) { return; }
        binding.closeRegistration = managedExecutorRegistry.onClose(binding::remove);
        binding.registration = managedExecutorRegistry.onRegistration(binding::add);
    }

    @Override
    public void destroy() {
        bindings.values().forEach(Binding::close);
        bindings.clear();
    }

    private final class Binding {
        private final MeterRegistry registry;
        private final Map<ManagedExecutor, List<Meter>> meters = new ConcurrentHashMap<>();
        private final Map<ManagedExecutor, AutoCloseable> timingSubscriptions = new ConcurrentHashMap<>();
        private final AtomicBoolean closed = new AtomicBoolean();
        private AutoCloseable registration;
        private AutoCloseable closeRegistration;

        private Binding(MeterRegistry registry) { this.registry = registry; }

        private synchronized void add(ManagedExecutor executor) {
            if (closed.get() || executor.isShutdown() || meters.containsKey(executor)) { return; }
            List<Meter> owned = new ArrayList<>();
            meters.put(executor, owned);
            adding.set(owned);
            try {
                ExecutorSnapshot snapshot = executor.snapshot();
                Tags tags = tags(snapshot);
                registerCommonMeters(registry, executor, tags);
                if (snapshot != null && snapshot.isVirtual()) { registerVirtualMeters(registry, executor, tags); }
                else { registerPlatformMeters(registry, executor, tags); }
                ownGauge("dtp.executor.queue.resizable", executor, value -> value.supportsQueueResize() ? 1 : 0, tags);
                ownGauge("dtp.executor.queue.capacity", executor, value -> {
                    ExecutorSnapshot state = value.snapshot();
                    return state == null || state.getQueueSize() == null || state.getRemainingCapacity() == null ? 0
                            : (double) state.getQueueSize() + state.getRemainingCapacity();
                }, tags);
                ownGauge("dtp.executor.observation.complete", executor,
                        value -> value.taskObserver() != null && value.taskObserver().isComplete() ? 1 : 0, tags);
                DtpTaskObserver observer = executor.taskObserver();
                if (observer != null) {
                    counter("dtp.task.submitted", observer, DtpTaskObserver::submittedTasks, tags);
                    counter("dtp.task.succeeded", observer, DtpTaskObserver::succeededTasks, tags);
                    counter("dtp.task.failed", observer, DtpTaskObserver::failedTasks, tags);
                    counter("dtp.task.cancelled", observer, DtpTaskObserver::cancelledTasks, tags);
                    counter("dtp.task.rejected", observer, DtpTaskObserver::rejectedTasks, tags);
                    Timer wait = timer("dtp.task.queue.wait", tags);
                    Timer execution = timer("dtp.task.execution", tags);
                    timingSubscriptions.put(executor, observer.recordTimings((kind, nanos) -> {
                        Timer timer = "wait".equals(kind) ? wait : execution;
                        if (timer != null) { timer.record(nanos, TimeUnit.NANOSECONDS); }
                    }));
                }
            } catch (RuntimeException e) {
                remove(executor);
                throw e;
            } finally { adding.remove(); }
        }

        private void ownGauge(String name, ManagedExecutor executor, java.util.function.ToDoubleFunction<ManagedExecutor> value, Tags tags) {
            if (registry.find(name).tags(tags).meter() == null) {
                adding.get().add(Gauge.builder(name, executor, value).tags(tags).register(registry));
            }
        }

        private void counter(String name, DtpTaskObserver observer, java.util.function.ToDoubleFunction<DtpTaskObserver> value, Tags tags) {
            if (registry.find(name).tags(tags).meter() == null) {
                adding.get().add(FunctionCounter.builder(name, observer, value).tags(tags).register(registry));
            }
        }

        private Timer timer(String name, Tags tags) {
            if (registry.find(name).tags(tags).meter() != null) { return null; }
            Timer timer = Timer.builder(name).tags(tags).register(registry);
            adding.get().add(timer);
            return timer;
        }

        private synchronized void remove(ManagedExecutor executor) {
            closeQuietly(timingSubscriptions.remove(executor));
            List<Meter> owned = meters.remove(executor);
            if (owned != null) { owned.forEach(registry::remove); }
        }

        private void close() {
            closed.set(true);
            closeQuietly(registration);
            closeQuietly(closeRegistration);
            List.copyOf(meters.keySet()).forEach(this::remove);
        }
    }

    private void closeQuietly(AutoCloseable subscription) {
        if (subscription != null) {
            try { subscription.close(); }
            catch (Exception e) { log.warn("动态线程池指标解绑失败 cause={}", e.getClass().getSimpleName()); }
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
        if (registry.find(name).tags(tags).meter() == null) {
            Meter meter = Gauge.builder(name, managedExecutor, executor -> value(executor, valueFunction))
                    .tags(tags).register(registry);
            List<Meter> owned = adding.get();
            if (owned != null) { owned.add(meter); }
        }
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
