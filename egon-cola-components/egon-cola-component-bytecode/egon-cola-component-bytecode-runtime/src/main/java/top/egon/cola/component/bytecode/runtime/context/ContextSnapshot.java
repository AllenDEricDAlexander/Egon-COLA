package top.egon.cola.component.bytecode.runtime.context;

import top.egon.cola.component.bytecode.api.executor.ContextCarrier;
import top.egon.cola.component.bytecode.api.executor.ContextScope;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class ContextSnapshot {

    private final List<CapturedContext> captured;

    ContextSnapshot(List<CapturedContext> captured) {
        this.captured = List.copyOf(captured);
    }

    public ContextScope restore() {
        Deque<ContextScope> scopes = new ArrayDeque<>();
        try {
            for (CapturedContext value : captured) {
                ContextScope scope = value.carrier.restore(value.snapshot);
                scopes.push(scope == null ? () -> { } : scope);
            }
            return () -> closeAll(scopes);
        } catch (RuntimeException | Error failure) {
            closeAfterFailure(scopes, failure);
            throw failure;
        }
    }

    private void closeAfterFailure(Deque<ContextScope> scopes, Throwable failure) {
        try {
            closeAll(scopes);
        } catch (RuntimeException | Error closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }

    private static void closeAll(Deque<ContextScope> scopes) {
        List<Throwable> failures = new ArrayList<>();
        while (!scopes.isEmpty()) {
            try {
                scopes.pop().close();
            } catch (RuntimeException | Error failure) {
                failures.add(failure);
            }
        }
        if (!failures.isEmpty()) {
            Throwable first = failures.getFirst();
            failures.stream().skip(1).forEach(first::addSuppressed);
            if (first instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw (Error) first;
        }
    }

    record CapturedContext(ContextCarrier carrier, Object snapshot) {
    }
}
