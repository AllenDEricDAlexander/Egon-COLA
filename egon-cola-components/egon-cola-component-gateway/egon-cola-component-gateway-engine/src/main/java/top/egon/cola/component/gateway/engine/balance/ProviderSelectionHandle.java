package top.egon.cola.component.gateway.engine.balance;

import top.egon.cola.component.gateway.core.provider.ProviderInstance;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ProviderSelectionHandle implements AutoCloseable {

    private final ProviderInstance instance;

    private final Runnable release;

    private final AtomicBoolean closed = new AtomicBoolean();

    public ProviderSelectionHandle(
            ProviderInstance instance,
            Runnable release) {
        this.instance = Objects.requireNonNull(instance, "instance");
        this.release = Objects.requireNonNull(release, "release");
    }

    public ProviderInstance instance() {
        return instance;
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            release.run();
        }
    }
}
