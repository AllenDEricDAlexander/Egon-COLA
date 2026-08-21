package top.egon.cola.component.rpc.consumer.channel;

import io.grpc.ManagedChannel;

/** Per-user lease over a pool-owned ManagedChannel. */
public final class RpcChannelLease implements AutoCloseable {

    private final RpcConsumerChannelPool.Entry entry;
    private boolean closed;
    private int outstandingCalls;

    RpcChannelLease(RpcConsumerChannelPool.Entry entry) {
        this.entry = entry;
    }

    public synchronized ManagedChannel channel() {
        requireOpen();
        return entry.channel();
    }

    public synchronized void beginCall() {
        requireOpen();
        entry.beginCall();
        outstandingCalls++;
    }

    public synchronized void endCall() {
        if (outstandingCalls == 0) {
            return;
        }
        outstandingCalls--;
        entry.endCall();
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        int calls = outstandingCalls;
        outstandingCalls = 0;
        for (int index = 0; index < calls; index++) {
            entry.endCall();
        }
        entry.releaseReference();
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("RPC channel lease is closed");
        }
    }
}
