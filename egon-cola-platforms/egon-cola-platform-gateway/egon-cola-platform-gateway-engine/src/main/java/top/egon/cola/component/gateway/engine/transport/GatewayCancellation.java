package top.egon.cola.component.gateway.engine.transport;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import reactor.core.Disposable;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Idempotent owner for subscriptions and buffers abandoned by cancellation.
 */
public final class GatewayCancellation implements AutoCloseable {

    private final AtomicBoolean cancelled = new AtomicBoolean();

    private final ConcurrentLinkedQueue<Disposable> resources =
            new ConcurrentLinkedQueue<>();

    private final Set<DataBuffer> buffers = ConcurrentHashMap.newKeySet();

    public void register(Disposable resource) {
        Objects.requireNonNull(resource, "resource");
        resources.add(resource);
        if (cancelled.get() && resources.remove(resource)) {
            resource.dispose();
        }
    }

    public boolean own(DataBuffer buffer) {
        Objects.requireNonNull(buffer, "buffer");
        if (cancelled.get()) {
            DataBufferUtils.release(buffer);
            return false;
        }
        buffers.add(buffer);
        if (cancelled.get() && buffers.remove(buffer)) {
            DataBufferUtils.release(buffer);
            return false;
        }
        return true;
    }

    public boolean transfer(DataBuffer buffer) {
        return buffer != null && buffers.remove(buffer);
    }

    public boolean cancel() {
        if (!cancelled.compareAndSet(false, true)) {
            return false;
        }
        Disposable resource;
        while ((resource = resources.poll()) != null) {
            resource.dispose();
        }
        buffers.forEach(DataBufferUtils::release);
        buffers.clear();
        return true;
    }

    public boolean cancelled() {
        return cancelled.get();
    }

    @Override
    public void close() {
        cancel();
    }
}
