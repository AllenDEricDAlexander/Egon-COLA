package top.egon.cola.component.gateway.core.exchange;

import top.egon.cola.component.gateway.core.http.GatewayRequestRejectedException;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AggregatedGatewayBody implements GatewayBody, AutoCloseable {

    private final byte[] content;

    private final boolean replayable;

    private final AtomicBoolean consumed = new AtomicBoolean();

    private final AtomicBoolean closed = new AtomicBoolean();

    public AggregatedGatewayBody(byte[] content, long maxBytes, boolean replayable) {
        if (maxBytes < 0) {
            throw new IllegalArgumentException("maxBytes must not be negative");
        }
        if (content == null) {
            throw new IllegalArgumentException("content is required");
        }
        if (content.length > maxBytes) {
            throw new GatewayRequestRejectedException(
                    "GATEWAY_REQUEST_BODY_TOO_LARGE",
                    413,
                    "request body exceeds configured limit"
            );
        }
        this.content = content.clone();
        this.replayable = replayable;
    }

    public byte[] consume() {
        if (closed.get()) {
            throw new IllegalStateException("body is closed");
        }
        if (!replayable && !consumed.compareAndSet(false, true)) {
            throw new IllegalStateException("body already consumed");
        }
        consumed.set(true);
        return content.clone();
    }

    @Override
    public long contentLength() {
        return content.length;
    }

    @Override
    public boolean replayable() {
        return replayable;
    }

    public boolean consumed() {
        return consumed.get();
    }

    public boolean closed() {
        return closed.get();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            Arrays.fill(content, (byte) 0);
        }
    }
}
