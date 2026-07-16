package top.egon.cola.component.bytecode.bridge;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DispatcherRegistration implements AutoCloseable {

    private final AtomicBoolean closed = new AtomicBoolean();
    private final Runnable closeAction;

    DispatcherRegistration(Runnable closeAction) {
        this.closeAction = Objects.requireNonNull(closeAction, "closeAction");
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            closeAction.run();
        }
    }
}
