package top.egon.cola.component.rpc.consumer.channel;

import io.grpc.ManagedChannel;
import org.springframework.context.SmartLifecycle;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** Owns one bounded ManagedChannel entry per stable transport key. */
public final class RpcConsumerChannelPool implements SmartLifecycle, AutoCloseable {

    private final RpcConsumerChannelFactory channelFactory;
    private final long drainTimeoutMs;
    private final Map<RpcChannelKey, Entry> entries = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lifecycleLock = new ReentrantReadWriteLock();
    private final ScheduledExecutorService drainExecutor =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "egon-rpc-channel-drain");
                thread.setDaemon(true);
                return thread;
            });
    private final AtomicBoolean closed = new AtomicBoolean();

    public RpcConsumerChannelPool(
            RpcConsumerChannelFactory channelFactory,
            Duration drainTimeout) {
        this.channelFactory = Objects.requireNonNull(channelFactory, "channelFactory");
        if (drainTimeout == null || drainTimeout.isZero() || drainTimeout.isNegative()) {
            throw new IllegalArgumentException("drainTimeout must be positive");
        }
        this.drainTimeoutMs = Math.max(1L, drainTimeout.toMillis());
    }

    public RpcConsumerChannelPool(
            RpcConsumerChannelFactory channelFactory,
            long drainTimeoutMs) {
        this(channelFactory, Duration.ofMillis(drainTimeoutMs));
    }

    public RpcChannelLease acquire(RpcEndpoint endpoint) {
        return acquire(RpcChannelKey.from(endpoint));
    }

    public RpcChannelLease acquire(RpcChannelKey key) {
        Objects.requireNonNull(key, "key");
        lifecycleLock.readLock().lock();
        try {
            if (closed.get()) {
                throw unavailable("RPC channel pool is draining");
            }
            Entry entry = entries.compute(key, (ignored, current) -> {
                if (current == null || current.isClosed()) {
                    ManagedChannel channel = channelFactory.create(key);
                    return new Entry(key, channel);
                }
                current.retainReference();
                return current;
            });
            return new RpcChannelLease(entry);
        } finally {
            lifecycleLock.readLock().unlock();
        }
    }

    @Override
    public void start() {
        // Pool is ready for direct programmatic acquire immediately after construction.
    }

    @Override
    public void stop() {
        close();
    }

    @Override
    public boolean isRunning() {
        return !closed.get();
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 60;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        lifecycleLock.writeLock().lock();
        try {
            entries.values().forEach(Entry::markPoolClosing);
        } finally {
            lifecycleLock.writeLock().unlock();
        }
        if (entries.isEmpty()) {
            drainExecutor.shutdownNow();
        }
    }

    private void remove(Entry entry) {
        entries.remove(entry.key(), entry);
        if (closed.get() && entries.isEmpty()) {
            drainExecutor.shutdown();
        }
    }

    private EgonRpcException unavailable(String message) {
        return new EgonRpcException(EgonRpcErrorCode.RPC_PROVIDER_UNAVAILABLE, message);
    }

    final class Entry {

        private final RpcChannelKey key;
        private final ManagedChannel channel;
        private int references = 1;
        private int inFlight;
        private boolean draining;
        private boolean forceClosed;
        private ScheduledFuture<?> forceTask;

        private Entry(RpcChannelKey key, ManagedChannel channel) {
            this.key = key;
            this.channel = Objects.requireNonNull(channel, "channel");
        }

        synchronized ManagedChannel channel() {
            return channel;
        }

        private synchronized boolean isClosed() {
            return draining && references == 0;
        }

        private synchronized void retainReference() {
            if (draining) {
                throw unavailable("RPC channel entry is draining");
            }
            references++;
        }

        synchronized void beginCall() {
            if (draining) {
                throw unavailable("RPC channel entry is draining");
            }
            inFlight++;
        }

        synchronized void endCall() {
            if (inFlight == 0) {
                return;
            }
            inFlight--;
            if (draining && references == 0 && inFlight == 0) {
                finishGracefully();
            }
        }

        synchronized void releaseReference() {
            if (references == 0) {
                return;
            }
            references--;
            if (references == 0) {
                if (!draining) {
                    beginDrain();
                } else {
                    RpcConsumerChannelPool.this.remove(this);
                    if (inFlight == 0) {
                        finishGracefully();
                    }
                }
            }
        }

        private synchronized void markPoolClosing() {
            if (!draining) {
                draining = true;
                channel.shutdown();
            }
            if (references == 0) {
                RpcConsumerChannelPool.this.remove(this);
                if (inFlight == 0) {
                    finishGracefully();
                } else {
                    scheduleForceClose();
                }
            } else if (inFlight > 0) {
                scheduleForceClose();
            }
        }

        private synchronized void beginDrain() {
            if (!draining) {
                draining = true;
                channel.shutdown();
            }
            RpcConsumerChannelPool.this.remove(this);
            if (inFlight == 0) {
                finishGracefully();
            } else {
                scheduleForceClose();
            }
        }

        private synchronized void finishGracefully() {
            if (forceClosed) {
                return;
            }
            if (forceTask != null) {
                forceTask.cancel(false);
                forceTask = null;
            }
            RpcConsumerChannelPool.this.remove(this);
        }

        private synchronized void scheduleForceClose() {
            if (forceTask == null) {
                forceTask = drainExecutor.schedule(
                        this::forceClose, drainTimeoutMs, TimeUnit.MILLISECONDS);
            }
        }

        private synchronized void forceClose() {
            if (forceClosed) {
                return;
            }
            forceClosed = true;
            forceTask = null;
            channel.shutdownNow();
            RpcConsumerChannelPool.this.remove(this);
        }

        private RpcChannelKey key() {
            return key;
        }
    }
}
