package top.egon.cola.component.dtp.executor;

import java.util.function.Supplier;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.dtp.exception.LoggingDtpTaskExceptionHandler;
import top.egon.cola.component.dtp.exception.DtpTaskExceptionHandler;
import top.egon.cola.component.dtp.context.DtpTaskContextManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import top.egon.cola.component.dtp.config.DynamicThreadPoolAutoProperties;
import top.egon.cola.component.dtp.context.DtpTaskObserver;
import top.egon.cola.component.dtp.domain.model.entity.ThreadPoolCreateCommand;
import top.egon.cola.component.dtp.executor.adapter.BoundedVirtualThreadManagedExecutor;
import top.egon.cola.component.dtp.executor.adapter.ThreadPoolExecutorManagedExecutor;
import top.egon.cola.component.dtp.executor.adapter.ThreadPoolTaskExecutorManagedExecutor;
import top.egon.cola.component.dtp.executor.managed.DtpThreadPoolTaskExecutor;
import top.egon.cola.component.dtp.executor.virtual.BoundedVirtualThreadExecutor;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @description 唯一执行器目录，名称、原对象和关闭归属原子登记
 */
@Slf4j
public class ManagedExecutorRegistry {

    private final Map<String, Registration> registrations = new LinkedHashMap<>();
    private final IdentityHashMap<Object, Registration> identities = new IdentityHashMap<>();
    private final CopyOnWriteArrayList<Consumer<ManagedExecutor>> listeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<ManagedExecutor>> closeListeners = new CopyOnWriteArrayList<>();
    private final String appName;
    private final String instanceId;
    private final DynamicThreadPoolAutoProperties properties;
    private final Supplier<DtpTaskContextManager> contexts;
    private final Supplier<DtpTaskExceptionHandler> handlers;
    private final Supplier<ValidationUtils> validation;
    private boolean closing;

    public ManagedExecutorRegistry(Collection<ManagedExecutor> managedExecutors) {
        this("default-app", "default-instance", new DynamicThreadPoolAutoProperties());
        if (managedExecutors != null) {
            for (ManagedExecutor executor : managedExecutors) {
                if (executor != null) {
                    Registration existing = registrations.get(executor.executorName());
                    if (existing != null) {
                        if (existing.delegate == executor) { continue; }
                        throw new IllegalStateException("executor name already registered: " + executor.executorName());
                    }
                    Registration registration = new Registration(executor, executor, null, false);
                    registrations.put(executor.executorName(), registration);
                    identities.put(executor, registration);
                }
            }
        }
    }

    public ManagedExecutorRegistry(String appName, String instanceId, DynamicThreadPoolAutoProperties properties) {
        this(appName, instanceId, properties, new DtpTaskContextManager());
    }

    private ManagedExecutorRegistry(String appName, String instanceId, DynamicThreadPoolAutoProperties properties,
                                   DtpTaskContextManager context) {
        this(appName, instanceId, properties, () -> context,
                LoggingDtpTaskExceptionHandler::new, () -> null);
    }

    public ManagedExecutorRegistry(String appName, String instanceId, DynamicThreadPoolAutoProperties properties,
            Supplier<DtpTaskContextManager> contexts,
            Supplier<DtpTaskExceptionHandler> handlers,
            Supplier<ValidationUtils> validation) {
        this.appName = Objects.requireNonNull(appName, "appName");
        this.instanceId = Objects.requireNonNull(instanceId, "instanceId");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.contexts = Objects.requireNonNull(contexts, "contexts");
        this.handlers = Objects.requireNonNull(handlers, "handlers");
        this.validation = Objects.requireNonNull(validation, "validation");
    }

    public synchronized Optional<ManagedExecutor> get(String executorName) {
        Registration registration = registrations.get(executorName);
        return Optional.ofNullable(registration == null ? null : registration.managed);
    }

    public synchronized Collection<ManagedExecutor> list() {
        return List.copyOf(new java.util.LinkedHashSet<>(registrations.values()).stream()
                .map(entry -> entry.managed).toList());
    }

    synchronized boolean createsRoot(ManagedExecutor executor) {
        return registrations.values().stream().anyMatch(entry ->
                entry.managed == executor && entry.delegate instanceof DtpThreadPoolTaskExecutor);
    }

    public synchronized ThreadPoolTaskExecutor create(ThreadPoolCreateCommand command,
                                                       Function<DtpTaskObserver, DtpThreadPoolTaskExecutor> factory) {
        requireOpen();
        PoolDefinition definition = PoolDefinition.from(command);
        Registration existing = registrations.get(command.getExecutorName());
        if (existing != null) {
            if (!definition.matches(existing.definition) || !(existing.delegate instanceof DtpThreadPoolTaskExecutor pool)
                    || pool.getThreadPoolExecutor().isShutdown()) {
                throw new IllegalStateException("executor name conflicts with an existing definition: " + command.getExecutorName());
            }
            return pool;
        }
        DtpTaskObserver observer = observer(command.getExecutorName(), true);
        DtpThreadPoolTaskExecutor pool = factory.apply(observer);
        try {
            Registration registration = new Registration(pool, adapt(command.getExecutorName(), pool, observer), definition, true);
            observer.setExecutorKind(registration.managed.kind());
            registrations.put(command.getExecutorName(), registration);
            identities.put(pool, registration);
            pool.onShutdown(() -> closeListeners.forEach(listener -> listener.accept(registration.managed)));
            notifyRegistered(registration.managed);
            return pool;
        } catch (RuntimeException | Error e) {
            pool.shutdown();
            throw e;
        }
    }

    public synchronized ManagedExecutor registerBean(String beanName, Object delegate) {
        requireOpen();
        Registration byName = registrations.get(beanName);
        Registration byIdentity = identities.get(delegate);
        if (byName != null && byName.delegate != delegate) {
            throw new IllegalStateException("executor name already belongs to another object: " + beanName);
        }
        if (byIdentity != null) {
            byIdentity.managerOwned = false;
            registrations.put(beanName, byIdentity);
            return byIdentity.managed;
        }
        DtpTaskObserver observer = delegate instanceof DtpThreadPoolTaskExecutor pool ? pool.taskObserver()
                : observer(beanName, false);
        Registration registration = new Registration(delegate, adapt(beanName, delegate, observer), null, false);
        observer.setExecutorKind(registration.managed.kind());
        registrations.put(beanName, registration);
        identities.put(delegate, registration);
        if (delegate instanceof DtpThreadPoolTaskExecutor pool) {
            pool.onShutdown(() -> closeListeners.forEach(listener -> listener.accept(registration.managed)));
        }
        notifyRegistered(registration.managed);
        return registration.managed;
    }

    public synchronized AutoCloseable onRegistration(Consumer<ManagedExecutor> listener) {
        listeners.add(listener);
        for (ManagedExecutor executor : list()) {
            try { listener.accept(executor); }
            catch (RuntimeException e) {
                log.warn("动态线程池初始指标登记失败 executor={} cause={}", executor.executorName(), e.getClass().getSimpleName());
            }
        }
        return () -> listeners.remove(listener);
    }

    public AutoCloseable onClose(Consumer<ManagedExecutor> listener) {
        closeListeners.add(listener);
        return () -> closeListeners.remove(listener);
    }

    public void closeOwned() {
        List<DtpThreadPoolTaskExecutor> owned;
        synchronized (this) {
            if (closing) { return; }
            closing = true;
            owned = identities.values().stream().filter(entry -> entry.managerOwned)
                    .map(entry -> (DtpThreadPoolTaskExecutor) entry.delegate).toList();
        }
        owned.forEach(DtpThreadPoolTaskExecutor::shutdown);
    }

    private ManagedExecutor adapt(String name, Object executor, DtpTaskObserver observer) {
        if (executor instanceof ThreadPoolTaskExecutor spring) {
            return new ThreadPoolTaskExecutorManagedExecutor(appName, instanceId, name, spring, observer);
        }
        if (executor instanceof ThreadPoolExecutor platform) {
            return new ThreadPoolExecutorManagedExecutor(appName, instanceId, name, platform, observer);
        }
        if (executor instanceof BoundedVirtualThreadExecutor virtual) {
            return new BoundedVirtualThreadManagedExecutor(appName, instanceId, name, virtual, observer);
        }
        throw new IllegalArgumentException("unsupported executor type: " + executor.getClass().getName());
    }

    private DtpTaskObserver observer(String name, boolean complete) {
        return new DtpTaskObserver(appName, instanceId, name, properties.getObservation(), complete,
                contexts, handlers, validation, properties.getExceptionHandling());
    }

    private void notifyRegistered(ManagedExecutor executor) {
        for (Consumer<ManagedExecutor> listener : listeners) {
            try { listener.accept(executor); }
            catch (RuntimeException e) {
                log.warn("动态线程池指标登记失败 executor={} cause={}", executor.executorName(), e.getClass().getSimpleName());
            }
        }
    }

    private void requireOpen() {
        if (closing) { throw new IllegalStateException("executor registry is closing"); }
    }

    private static final class Registration {
        private final Object delegate;
        private final ManagedExecutor managed;
        private final PoolDefinition definition;
        private boolean managerOwned;

        private Registration(Object delegate, ManagedExecutor managed, PoolDefinition definition, boolean managerOwned) {
            this.delegate = delegate;
            this.managed = managed;
            this.definition = definition;
            this.managerOwned = managerOwned;
        }
    }

    private record PoolDefinition(int core, int max, int queue, long keepAliveSeconds, boolean allowCoreTimeout,
                                   java.util.concurrent.ThreadFactory threadFactory,
                                   java.util.concurrent.RejectedExecutionHandler handler) {
        private boolean matches(PoolDefinition other) {
            return other != null && core == other.core && max == other.max && queue == other.queue
                    && keepAliveSeconds == other.keepAliveSeconds && allowCoreTimeout == other.allowCoreTimeout
                    && threadFactory == other.threadFactory && handler == other.handler;
        }

        private static PoolDefinition from(ThreadPoolCreateCommand command) {
            return new PoolDefinition(command.getCorePoolSize(), command.getMaximumPoolSize(), command.getQueueCapacity(),
                    command.getKeepAlive().getSeconds(), command.isAllowCoreThreadTimeOut(),
                    command.getThreadFactory(), command.getRejectedExecutionHandler());
        }
    }
}
