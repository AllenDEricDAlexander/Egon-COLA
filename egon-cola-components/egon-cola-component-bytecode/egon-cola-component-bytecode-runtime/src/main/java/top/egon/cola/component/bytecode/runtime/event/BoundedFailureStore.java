package top.egon.cola.component.bytecode.runtime.event;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

public final class BoundedFailureStore {

    private final int capacity;
    private final Deque<RuntimeFailure> failures = new ArrayDeque<>();

    public BoundedFailureStore(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must not be negative");
        }
        this.capacity = capacity;
    }

    public synchronized void record(String source, Throwable failure) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(failure, "failure");
        if (capacity == 0) {
            return;
        }
        while (failures.size() >= capacity) {
            failures.removeFirst();
        }
        failures.addLast(new RuntimeFailure(
                System.nanoTime(), source, failure.getClass().getName()));
    }

    public synchronized List<RuntimeFailure> failures() {
        return List.copyOf(failures);
    }

    public record RuntimeFailure(long occurredNanos, String source, String exceptionType) {
    }
}
