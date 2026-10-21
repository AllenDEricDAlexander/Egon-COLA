package top.egon.cola.component.dtp.context;

import io.micrometer.context.ThreadLocalAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.dtp.exception.DtpContextPropagationException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;

class DtpTaskContextManagerTest {
    private final ThreadLocal<String> first = new ThreadLocal<>();
    private final ThreadLocal<String> second = new ThreadLocal<>();

    @AfterEach
    void clear() {
        first.remove();
        second.remove();
        MDC.clear();
    }

    private static ThreadLocalAccessor<String> accessor(String key, ThreadLocal<String> local) {
        return new ThreadLocalAccessor<>() {
            public Object key() { return key; }
            public String getValue() { return local.get(); }
            public void setValue(String value) { local.set(value); }
            public void setValue() { local.remove(); }
        };
    }

    @Test
    void snapshotAndEmptyValuesRestoreThePreviousThreadIncludingNestedScopes() {
        var manager = new DtpTaskContextManager(List.of(accessor("first", first), accessor("second", second)));
        first.set("request");
        MDC.put("traceId", "request-trace");
        var request = manager.capture();
        first.remove();
        MDC.clear();
        var empty = manager.capture();
        first.set("worker");
        second.set("worker-second");
        MDC.put("worker", "original");
        try (var outer = manager.open(request)) {
            assertThat(first.get()).isEqualTo("request");
            assertThat(second.get()).isNull();
            assertThat(MDC.get("traceId")).isEqualTo("request-trace");
            try (var inner = manager.open(empty)) {
                assertThat(first.get()).isNull();
                assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
            }
            assertThat(first.get()).isEqualTo("request");
        }
        assertThat(first.get()).isEqualTo("worker");
        assertThat(second.get()).isEqualTo("worker-second");
        assertThat(MDC.getCopyOfContextMap()).containsExactlyEntriesOf(Map.of("worker", "original"));
    }

    @Test
    void partialInstallRollsBackTheAccessorThatMutatedBeforeThrowing() {
        RuntimeException original = new IllegalStateException("install failed");
        ThreadLocalAccessor<String> broken = new ThreadLocalAccessor<>() {
            public Object key() { return "second"; }
            public String getValue() { return second.get(); }
            public void setValue(String value) {
                second.set(value);
                if ("request".equals(value)) { throw original; }
            }
            public void setValue() { second.remove(); }
        };
        var manager = new DtpTaskContextManager(List.of(accessor("first", first), broken));
        first.set("request");
        second.set("request");
        var snapshot = manager.capture();
        first.set("worker");
        second.set("worker");
        MDC.put("traceId", "worker-trace");
        assertThatThrownBy(() -> manager.open(snapshot)).isInstanceOf(DtpContextPropagationException.class).hasCause(original);
        assertThat(first.get()).isEqualTo("worker");
        assertThat(second.get()).isEqualTo("worker");
        assertThat(MDC.get("traceId")).isEqualTo("worker-trace");
    }

    @Test
    void brokenRestoreDoesNotPreventOtherRestoresOrChangeBusinessFailure() {
        ThreadLocalAccessor<String> broken = new ThreadLocalAccessor<>() {
            public Object key() { return "second"; }
            public String getValue() { return second.get(); }
            public void setValue(String value) { second.set(value); }
            public void setValue() { second.remove(); }
            public void restore(String previous) { throw new IllegalStateException("restore failed"); }
        };
        var manager = new DtpTaskContextManager(List.of(accessor("first", first), broken));
        first.set("request");
        second.set("request");
        var snapshot = manager.capture();
        first.set("worker");
        second.set("worker");
        MDC.put("traceId", "worker-trace");
        RuntimeException business = new IllegalArgumentException("business");
        assertThatThrownBy(() -> {
            try (var ignored = manager.open(snapshot)) { throw business; }
        }).isSameAs(business);
        assertThat(business.getSuppressed()).isEmpty();
        assertThat(first.get()).isEqualTo("worker");
        assertThat(MDC.get("traceId")).isEqualTo("worker-trace");
    }

    @Test
    void registrationRejectsDuplicateReservedKeysAndMissingClear() {
        assertThatThrownBy(() -> new DtpTaskContextManager(List.of(accessor("same", first), accessor("same", second))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DtpTaskContextManager(List.of(new ThreadLocalAccessor<String>() {
            public Object key() { return TraceContext.class; }
            public String getValue() { return first.get(); }
            public void setValue(String value) { first.set(value); }
            public void setValue() { first.remove(); }
        }))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DtpTaskContextManager(List.of(new ThreadLocalAccessor<String>() {
            public Object key() { return "missing-clear"; }
            public String getValue() { return first.get(); }
            public void setValue(String value) { first.set(value); }
        }))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void scopeIsThreadConfinedIdempotentAndRejectsOtherManagers() throws Exception {
        var manager = new DtpTaskContextManager(List.of(accessor("first", first)));
        var snapshot = manager.capture();
        assertThatThrownBy(() -> new DtpTaskContextManager().open(snapshot)).isInstanceOf(IllegalArgumentException.class);
        var scope = manager.open(snapshot);
        try (var other = Executors.newSingleThreadExecutor()) {
            assertThat(other.submit(() -> {
                try { scope.close(); return false; }
                catch (IllegalStateException expected) { return true; }
            }).get()).isTrue();
        } finally {
            scope.close();
            scope.close();
        }
    }

    @Test
    void captureFailureNeverChangesTheSubmittingContext() {
        var manager = new DtpTaskContextManager(List.of(new ThreadLocalAccessor<String>() {
            public Object key() { return "broken-reader"; }
            public String getValue() { throw new IllegalStateException("capture failed"); }
            public void setValue(String value) { }
            public void setValue() { }
        }));
        MDC.put("traceId", "caller");
        assertThatThrownBy(manager::capture).isInstanceOf(DtpContextPropagationException.class);
        assertThat(MDC.get("traceId")).isEqualTo("caller");
    }

    @Test
    void accessorDefensiveCopiesRemainIndependentFromLaterParentMutation() {
        ThreadLocal<java.util.Map<String, String>> data = new ThreadLocal<>();
        var manager = new DtpTaskContextManager(List.of(new ThreadLocalAccessor<java.util.Map<String, String>>() {
            public Object key() { return "copy"; }
            public java.util.Map<String, String> getValue() {
                return data.get() == null ? null : Map.copyOf(data.get());
            }
            public void setValue(java.util.Map<String, String> value) { data.set(value); }
            public void setValue() { data.remove(); }
        }));
        var original = new java.util.HashMap<>(Map.of("value", "A"));
        data.set(original);
        var snapshot = manager.capture();
        original.put("value", "B");
        data.remove();
        try (var ignored = manager.open(snapshot)) {
            assertThat(data.get()).containsEntry("value", "A");
        } finally { data.remove(); }
    }

    @Test
    void concurrentScopesUseIndependentThreadStateWithTheSameManager() throws Exception {
        var manager = new DtpTaskContextManager(List.of(accessor("first", first)));
        first.set("A"); MDC.put("traceId", "trace-A");
        var left = manager.capture();
        first.set("B"); MDC.put("traceId", "trace-B");
        var right = manager.capture();
        try (var workers = Executors.newFixedThreadPool(2)) {
            var jobs = java.util.stream.IntStream.range(0, 2).mapToObj(index -> (java.util.concurrent.Callable<Void>) () -> {
                var snapshot = index == 0 ? left : right;
                String expected = index == 0 ? "A" : "B";
                for (int iteration = 0; iteration < 50; iteration++) {
                    try (var ignored = manager.open(snapshot)) {
                        assertThat(first.get()).isEqualTo(expected);
                        assertThat(MDC.get("traceId")).isEqualTo("trace-" + expected);
                        first.set("task-mutation");
                    }
                    assertThat(first.get()).isNull();
                    assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
                }
                return null;
            }).toList();
            for (var result : workers.invokeAll(jobs)) { result.get(3, java.util.concurrent.TimeUnit.SECONDS); }
        }
    }

    @Test
    void diagnosticKeysAreFrozenAtRegistrationEvenWhenAccessorLaterMisbehaves() {
        var registered = new java.util.concurrent.atomic.AtomicBoolean();
        RuntimeException install = new IllegalStateException("install");
        var accessor = new ThreadLocalAccessor<String>() {
            public Object key() {
                if (registered.get()) { throw new IllegalStateException("late key failure"); }
                return "stable-key";
            }
            public String getValue() { return first.get(); }
            public void setValue(String value) {
                first.set(value);
                if ("request".equals(value)) { throw install; }
            }
            public void setValue() { first.remove(); }
        };
        var manager = new DtpTaskContextManager(List.of(accessor));
        registered.set(true);
        first.set("request");
        var snapshot = manager.capture();
        first.set("worker");
        assertThatThrownBy(() -> manager.open(snapshot)).hasCause(install);
        assertThat(first.get()).isEqualTo("worker");
    }
}
