package top.egon.cola.component.rpc.ddc.client;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 同时持有 DDC Port 客户端和其专属 Direct Channel 的句柄。
 * / Handle owning a DDC Port client and its dedicated Direct channel.
 */
public final class DdcRpcClientHandle<T> implements AutoCloseable {

    private final T client;
    private final AutoCloseable owner;
    private final AtomicBoolean closed = new AtomicBoolean();

    public DdcRpcClientHandle(T client, AutoCloseable owner) {
        this.client = Objects.requireNonNull(client, "client");
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    public T client() {
        return client;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        try {
            owner.close();
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("failed to close DDC RPC client", exception);
        }
    }
}
