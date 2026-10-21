package top.egon.cola.component.dtp.context;

import io.micrometer.context.ThreadLocalAccessor;
import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.common.core.exception.ExceptionLogger;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.dtp.exception.DtpContextPropagationException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.time.Duration;
import java.util.IdentityHashMap;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

/** 应用内显式登记的上下文；Scope只在实际执行线程安装和恢复。 */
@Slf4j
public class DtpTaskContextManager {
    private final List<ThreadLocalAccessor<?>> accessors;
    private final Object identity = new Object();
    private final Map<ThreadLocalAccessor<?>, String> accessorKeys;
    private final long diagnosticInterval;
    private final AtomicLong lastDiagnostic = new AtomicLong(Long.MIN_VALUE);
    private final AtomicLong suppressedDiagnostics = new AtomicLong();

    public DtpTaskContextManager() {
        this(List.of());
    }

    public DtpTaskContextManager(Collection<? extends ThreadLocalAccessor<?>> accessors) {
        this(accessors, Duration.ofSeconds(1));
    }

    public DtpTaskContextManager(Collection<? extends ThreadLocalAccessor<?>> accessors, Duration diagnosticInterval) {
        this.accessors = List.copyOf(Objects.requireNonNull(accessors, "accessors"));
        if (diagnosticInterval == null || diagnosticInterval.isNegative() || diagnosticInterval.isZero()
                || diagnosticInterval.compareTo(Duration.ofNanos(Long.MAX_VALUE)) > 0) {
            throw new IllegalArgumentException("context diagnostic interval must be positive and fit nanoseconds");
        }
        this.diagnosticInterval = diagnosticInterval.toNanos();
        Set<Object> keys = new HashSet<>();
        Map<ThreadLocalAccessor<?>, String> registeredKeys = new IdentityHashMap<>();
        for (ThreadLocalAccessor<?> accessor : this.accessors) {
            Object key = accessor.key();
            if (!(key instanceof String || key instanceof Class<?>)
                    || key instanceof String text && (text.isBlank() || text.length() > 128
                    || text.chars().anyMatch(Character::isISOControl))
                    || key == TraceContext.class || "cp.slf4j".equals(key) || !keys.add(key)) {
                throw new IllegalArgumentException("invalid, reserved or duplicate task context accessor key");
            }
            registeredKeys.put(accessor, key instanceof Class<?> type ? type.getName() : key.toString());
            try {
                if (accessor.getClass().getMethod("setValue").getDeclaringClass() == ThreadLocalAccessor.class
                        && accessor.getClass().getMethod("reset").getDeclaringClass() == ThreadLocalAccessor.class) {
                    throw new IllegalArgumentException("task context accessor must implement clearing: " + key);
                }
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException("invalid task context accessor contract", e);
            }
        }
        this.accessorKeys = Collections.unmodifiableMap(registeredKeys);
    }

    public DtpTaskContextBO capture() {
        return capture(TraceContext.capture());
    }

    DtpTaskContextBO capture(TraceContext trace) {
        List<DtpTaskContextBO.AccessorValueBO> values = new ArrayList<>(accessors.size());
        for (ThreadLocalAccessor<?> accessor : accessors) {
            try {
                Object value = accessor.getValue();
                values.add(new DtpTaskContextBO.AccessorValueBO(accessor, value != null, value));
            } catch (Throwable e) {
                throw failure("CAPTURE", accessor, e);
            }
        }
        return new DtpTaskContextBO(Objects.requireNonNull(trace, "trace"), identity, List.copyOf(values));
    }

    public Scope open(DtpTaskContextBO snapshot) {
        return open(snapshot, ignored -> { });
    }

    Scope open(DtpTaskContextBO snapshot, Consumer<DtpContextPropagationException> diagnostic) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (snapshot.getRegistrationIdentity() != identity || snapshot.getTraceContext() == null
                || snapshot.getValues() == null || snapshot.getValues().size() != accessors.size()) {
            throw new IllegalArgumentException("task context snapshot belongs to a different registration");
        }
        List<DtpTaskContextBO.AccessorValueBO> previous = new ArrayList<>(accessors.size());
        for (int index = 0; index < accessors.size(); index++) {
            ThreadLocalAccessor<?> accessor = accessors.get(index);
            if (snapshot.getValues().get(index).getAccessor() != accessor) {
                throw new IllegalArgumentException("task context snapshot accessor mismatch");
            }
            try {
                Object value = accessor.getValue();
                previous.add(new DtpTaskContextBO.AccessorValueBO(accessor, value != null, value));
            } catch (Throwable e) {
                DtpContextPropagationException failure = failure("BACKUP", accessor, e);
                diagnostic.accept(failure);
                throw failure;
            }
        }
        Scope scope = new Scope(snapshot, previous, diagnostic);
        try {
            scope.traceScope = snapshot.getTraceContext().open();
            for (DtpTaskContextBO.AccessorValueBO value : snapshot.getValues()) {
                scope.attempted++;
                install(value);
            }
            return scope;
        } catch (Throwable e) {
            scope.close();
            DtpContextPropagationException failure = failure("INSTALL",
                    scope.attempted == 0 ? null : accessors.get(scope.attempted - 1), e);
            diagnostic.accept(failure);
            throw failure;
        }
    }

    @SuppressWarnings("unchecked")
    private static void install(DtpTaskContextBO.AccessorValueBO value) {
        ThreadLocalAccessor<Object> accessor = (ThreadLocalAccessor<Object>) value.getAccessor();
        if (value.isPresent()) { accessor.setValue(value.getValue()); }
        else { accessor.setValue(); }
    }

    @SuppressWarnings("unchecked")
    private static void restore(DtpTaskContextBO.AccessorValueBO value) {
        ThreadLocalAccessor<Object> accessor = (ThreadLocalAccessor<Object>) value.getAccessor();
        if (value.isPresent()) { accessor.restore(value.getValue()); }
        else { accessor.restore(); }
    }

    private DtpContextPropagationException failure(String phase, ThreadLocalAccessor<?> accessor, Throwable cause) {
        if (cause instanceof VirtualMachineError fatal) { throw fatal; }
        if (cause instanceof ThreadDeath fatal) { throw fatal; }
        return new DtpContextPropagationException(phase, accessor == null ? "mdc" : accessorKeys.get(accessor), cause);
    }

    private void report(DtpContextPropagationException failure, TraceContext trace) {
        long now = System.nanoTime();
        long previous = lastDiagnostic.get();
        if ((previous != Long.MIN_VALUE && now - previous < diagnosticInterval) || !lastDiagnostic.compareAndSet(previous, now)) {
            suppressedDiagnostics.incrementAndGet();
            return;
        }
        try (TraceContext.Scope ignored = trace.open()) {
            ExceptionLogger.log(log, failure, Map.of("contextPhase", failure.getPhase(), "accessorKey", failure.getAccessorKey(),
                    "suppressedCount", Long.toString(suppressedDiagnostics.getAndSet(0))));
        } catch (RuntimeException e) {
            // 诊断出口本身不可覆盖原业务结果；这里没有上下文值或原异常消息。
            try { log.error("动态线程池上下文诊断失败 phase={} cause={}", failure.getPhase(), e.getClass().getName()); }
            catch (RuntimeException ignored) { /* 日志后端已经不可用，仍继续剩余恢复。 */ }
        }
    }

    void diagnostic(DtpContextPropagationException failure, TraceContext trace) {
        report(failure, trace);
    }

    public final class Scope implements AutoCloseable {
        private final Thread thread = Thread.currentThread();
        private final DtpTaskContextBO snapshot;
        private final List<DtpTaskContextBO.AccessorValueBO> previous;
        private final Consumer<DtpContextPropagationException> diagnostic;
        private TraceContext.Scope traceScope;
        private int attempted;
        private boolean closed;

        private Scope(DtpTaskContextBO snapshot, List<DtpTaskContextBO.AccessorValueBO> previous,
                      Consumer<DtpContextPropagationException> diagnostic) {
            this.snapshot = snapshot;
            this.previous = previous;
            this.diagnostic = diagnostic;
        }

        @Override
        public void close() {
            if (Thread.currentThread() != thread) {
                throw new IllegalStateException("task context scope must close on its installing thread");
            }
            if (closed) { return; }
            closed = true;
            try {
                for (int index = attempted - 1; index >= 0; index--) {
                    DtpTaskContextBO.AccessorValueBO value = previous.get(index);
                    try { restore(value); }
                    catch (Throwable e) {
                        DtpContextPropagationException failure = failure("RESTORE", value.getAccessor(), e);
                        report(failure, snapshot.getTraceContext());
                        diagnostic.accept(failure);
                    }
                }
            } finally {
                try {
                    if (traceScope != null) { traceScope.close(); }
                } catch (Throwable e) {
                    DtpContextPropagationException failure = failure("RESTORE", null, e);
                    report(failure, snapshot.getTraceContext());
                    diagnostic.accept(failure);
                } finally { previous.clear(); }
            }
        }
    }
}
