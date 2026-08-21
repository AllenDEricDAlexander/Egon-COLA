package top.egon.cola.component.rpc.consumer.generic;

import top.egon.cola.component.rpc.consumer.reference.RpcReferenceDefinition;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceStrategy;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceStrategyFactory;
import top.egon.cola.component.rpc.consumer.provider.RpcProviderQuery;
import top.egon.cola.component.rpc.consumer.loadbalance.RpcLoadBalancer;
import top.egon.cola.component.rpc.consumer.loadbalance.RpcLoadBalancers;
import top.egon.cola.component.rpc.contract.identity.RpcServiceIdentity;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;

/** Bounded LRU/idle cache for generic fixed-mode discovery strategies. */
public final class RpcGenericTargetCache implements AutoCloseable {

    @FunctionalInterface
    public interface StrategyCreator {

        RpcReferenceStrategy create(RpcGenericInvocation invocation);
    }

    private final StrategyCreator strategyCreator;
    private final int maxEntries;
    private final long idleTimeoutMs;
    private final LongSupplier clockMs;
    private final RpcLoadBalancers loadBalancers;
    private final Map<Key, Entry> entries = new LinkedHashMap<>(
            16,
            0.75f,
            true
    );
    private boolean closed;

    public RpcGenericTargetCache(
            StrategyCreator strategyCreator,
            int maxEntries,
            Duration idleTimeout) {
        this(strategyCreator, maxEntries, idleTimeout, System::currentTimeMillis);
    }

    public RpcGenericTargetCache(
            StrategyCreator strategyCreator,
            int maxEntries,
            Duration idleTimeout,
            LongSupplier clockMs) {
        this(
                strategyCreator,
                maxEntries,
                idleTimeout,
                clockMs,
                new RpcLoadBalancers()
        );
    }

    public RpcGenericTargetCache(
            StrategyCreator strategyCreator,
            int maxEntries,
            Duration idleTimeout,
            LongSupplier clockMs,
            RpcLoadBalancers loadBalancers) {
        this.strategyCreator = Objects.requireNonNull(
                strategyCreator,
                "strategyCreator"
        );
        if (maxEntries < 1) {
            throw new IllegalArgumentException("maxEntries must be positive");
        }
        if (idleTimeout == null || idleTimeout.toMillis() < 1_000) {
            throw new IllegalArgumentException(
                    "idleTimeout must be at least one second"
            );
        }
        this.maxEntries = maxEntries;
        this.idleTimeoutMs = idleTimeout.toMillis();
        this.clockMs = Objects.requireNonNull(clockMs, "clockMs");
        this.loadBalancers = Objects.requireNonNull(loadBalancers, "loadBalancers");
    }

    public RpcGenericTargetCache(
            RpcReferenceStrategyFactory strategyFactory,
            int maxEntries,
            Duration idleTimeout) {
        this(
                invocation -> strategyFactory.create(definition(invocation)),
                maxEntries,
                idleTimeout,
                System::currentTimeMillis,
                new RpcLoadBalancers()
        );
    }

    public synchronized Entry resolve(RpcGenericInvocation invocation) {
        Objects.requireNonNull(invocation, "invocation");
        if (closed) {
            throw new IllegalStateException("generic target cache is closed");
        }
        Key key = Key.from(invocation);
        Entry entry = entries.get(key);
        if (entry == null || entry.closed()) {
            RpcReferenceStrategy strategy = strategyCreator.create(invocation);
            entry = new Entry(
                    key,
                    strategy,
                    loadBalancers.loadBalancer(invocation.loadBalance()),
                    clockMs.getAsLong()
            );
            entries.put(key, entry);
        }
        entry.retain(clockMs.getAsLong());
        evictOverflow();
        return entry;
    }

    public synchronized void evictIdle() {
        long cutoff = clockMs.getAsLong() - idleTimeoutMs;
        Iterator<Map.Entry<Key, Entry>> iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next().getValue();
            if (entry.inUse() == 0 && entry.lastAccessMs() <= cutoff) {
                iterator.remove();
                entry.closeOnce();
            }
        }
    }

    public synchronized int size() {
        return entries.size();
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        List<Entry> closing = new ArrayList<>(entries.values());
        entries.clear();
        closing.forEach(Entry::closeOnce);
    }

    private void evictOverflow() {
        while (entries.size() > maxEntries) {
            Entry candidate = entries.values().stream()
                    .filter(entry -> entry.inUse() == 0)
                    .findFirst()
                    .orElse(null);
            if (candidate == null) {
                return;
            }
            entries.remove(candidate.key(), candidate);
            candidate.closeOnce();
        }
    }

    private synchronized void release(Entry entry) {
        if (entry.inUse() > 0) {
            entry.release(clockMs.getAsLong());
        }
        evictOverflow();
    }

    private static RpcReferenceDefinition definition(
            RpcGenericInvocation invocation) {
        RpcServiceIdentity serviceIdentity = new RpcServiceIdentity(
                invocation.serviceName(),
                invocation.group(),
                invocation.version()
        );
        RpcProviderQuery query = invocation.mode()
                == top.egon.cola.component.rpc.consumer.reference.RpcReferenceMode.DIRECT
                ? new RpcProviderQuery(
                        invocation.bizCode(),
                        invocation.appCode(),
                        invocation.env(),
                        invocation.serviceName(),
                        invocation.group(),
                        invocation.version(),
                        "grpc"
                ) : null;
        return new RpcReferenceDefinition(
                invocation.mode(),
                serviceIdentity,
                query,
                Map.of()
        );
    }

    private record Key(
            top.egon.cola.component.rpc.consumer.reference.RpcReferenceMode mode,
            String bizCode,
            String appCode,
            String env,
            String serviceName,
            String group,
            String version,
            String fullMethodName,
            long timeoutMs,
            int retries,
            top.egon.cola.component.rpc.annotation.LoadBalance loadBalance,
            top.egon.cola.component.rpc.annotation.FailStrategy failStrategy
    ) {

        private static Key from(RpcGenericInvocation invocation) {
            return new Key(
                    invocation.mode(),
                    invocation.bizCode(),
                    invocation.appCode(),
                    invocation.env(),
                    invocation.serviceName(),
                    invocation.group(),
                    invocation.version(),
                    invocation.fullMethodName(),
                    invocation.timeoutMs(),
                    invocation.retries(),
                    invocation.loadBalance(),
                    invocation.failStrategy()
            );
        }
    }

    public final class Entry implements AutoCloseable {

        private final Key key;
        private final RpcReferenceStrategy strategy;
        private final RpcLoadBalancer loadBalancer;
        private int inUse;
        private long lastAccessMs;
        private boolean closed;

        private Entry(
                Key key,
                RpcReferenceStrategy strategy,
                RpcLoadBalancer loadBalancer,
                long lastAccessMs) {
            this.key = key;
            this.strategy = Objects.requireNonNull(strategy, "strategy");
            this.loadBalancer = Objects.requireNonNull(loadBalancer, "loadBalancer");
            this.lastAccessMs = lastAccessMs;
        }

        public RpcReferenceStrategy strategy() {
            return strategy;
        }

        public RpcLoadBalancer loadBalancer() {
            return loadBalancer;
        }

        public synchronized int inUse() {
            return inUse;
        }

        private synchronized long lastAccessMs() {
            return lastAccessMs;
        }

        private synchronized boolean closed() {
            return closed;
        }

        private synchronized void retain(long now) {
            if (closed) {
                throw new IllegalStateException("generic target cache entry is closed");
            }
            inUse++;
            lastAccessMs = now;
        }

        private synchronized void release(long now) {
            inUse = Math.max(0, inUse - 1);
            lastAccessMs = now;
        }

        public void release() {
            RpcGenericTargetCache.this.release(this);
        }

        @Override
        public void close() {
            release();
        }

        private synchronized void closeOnce() {
            if (closed) {
                return;
            }
            closed = true;
            try {
                loadBalancer.close();
            } finally {
                strategy.close();
            }
        }

        private Key key() {
            return key;
        }
    }
}
