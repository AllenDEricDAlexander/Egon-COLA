package top.egon.cola.component.gateway.engine.rpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public final class RpcProviderChannelCache implements AutoCloseable {

    private final Map<RpcProviderChannelKey, Entry> entries =
            new ConcurrentHashMap<>();

    private final Function<RpcProviderChannelKey, ManagedChannel> factory;

    private final Duration drainTimeout;

    public RpcProviderChannelCache(Duration drainTimeout) {
        this(drainTimeout, RpcProviderChannelCache::createChannel);
    }

    RpcProviderChannelCache(
            Duration drainTimeout,
            Function<RpcProviderChannelKey, ManagedChannel> factory) {
        this.drainTimeout = drainTimeout;
        this.factory = factory;
    }

    public ChannelHandle acquire(ProviderInstance provider) {
        RpcProviderChannelKey key = RpcProviderChannelKey.from(provider);
        Entry entry = entries.compute(key, (ignored, current) -> {
            Entry selected = current;
            if (selected == null || selected.draining.get()) {
                selected = new Entry(factory.apply(key));
            }
            selected.inFlight.incrementAndGet();
            return selected;
        });
        return new ChannelHandle(
                key,
                entry.channel,
                () -> release(key, entry)
        );
    }

    public void retainOnly(Set<RpcProviderChannelKey> activeKeys) {
        entries.forEach((key, entry) -> {
            if (!activeKeys.contains(key)
                    && entry.draining.compareAndSet(false, true)) {
                if (entry.inFlight.get() == 0) {
                    closeEntry(key, entry);
                }
            }
        });
    }

    public int size() {
        return entries.size();
    }

    public long drainingCount() {
        return entries.values().stream()
                .filter(entry -> entry.draining.get())
                .count();
    }

    @Override
    public void close() {
        entries.forEach((key, entry) -> {
            entry.draining.set(true);
            closeEntry(key, entry);
        });
        entries.clear();
    }

    private void release(RpcProviderChannelKey key, Entry entry) {
        if (entry.inFlight.decrementAndGet() == 0 && entry.draining.get()) {
            closeEntry(key, entry);
        }
    }

    private void closeEntry(RpcProviderChannelKey key, Entry entry) {
        if (entries.remove(key, entry)) {
            entry.channel.shutdown();
            try {
                if (!entry.channel.awaitTermination(
                        drainTimeout.toMillis(),
                        TimeUnit.MILLISECONDS
                )) {
                    entry.channel.shutdownNow();
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                entry.channel.shutdownNow();
            }
        }
    }

    private static ManagedChannel createChannel(RpcProviderChannelKey key) {
        ManagedChannelBuilder<?> builder = ManagedChannelBuilder
                .forAddress(key.host(), key.port());
        if (!key.secure()) {
            builder.usePlaintext();
        }
        return builder.build();
    }

    private static final class Entry {

        private final ManagedChannel channel;

        private final AtomicInteger inFlight = new AtomicInteger();

        private final AtomicBoolean draining = new AtomicBoolean();

        private Entry(ManagedChannel channel) {
            this.channel = channel;
        }
    }

    public static final class ChannelHandle implements AutoCloseable {

        private final RpcProviderChannelKey key;

        private final ManagedChannel channel;

        private final Runnable release;

        private final AtomicBoolean closed = new AtomicBoolean();

        private ChannelHandle(
                RpcProviderChannelKey key,
                ManagedChannel channel,
                Runnable release) {
            this.key = key;
            this.channel = channel;
            this.release = release;
        }

        public RpcProviderChannelKey key() {
            return key;
        }

        public ManagedChannel channel() {
            return channel;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                release.run();
            }
        }
    }
}
