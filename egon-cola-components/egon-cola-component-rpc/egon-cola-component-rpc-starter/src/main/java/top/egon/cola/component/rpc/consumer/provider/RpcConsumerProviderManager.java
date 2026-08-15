package top.egon.cola.component.rpc.consumer.provider;

import io.grpc.ManagedChannel;
import org.springframework.context.SmartLifecycle;
import top.egon.cola.component.rpc.config.EgonRpcProperties;
import top.egon.cola.component.rpc.consumer.channel.RpcConsumerChannelFactory;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 按精确服务查询共享订阅，并管理直连 Provider Channel 生命周期。
 *
 * <p>Shares subscriptions by exact service query and manages the lifecycle of
 * direct Provider channels.
 */
public class RpcConsumerProviderManager implements SmartLifecycle {

    private final Object monitor = new Object();

    private final RpcProviderDirectory directory;

    private final RpcConsumerChannelFactory channelFactory;

    private final EgonRpcProperties.Consumer properties;

    private final Map<RpcProviderQuery, Registration> registrations =
            new LinkedHashMap<>();

    private final Set<ManagedChannel> drainingChannels =
            Collections.newSetFromMap(new IdentityHashMap<>());

    private volatile boolean running;

    private volatile ScheduledExecutorService drainExecutor;

    public RpcConsumerProviderManager(
            RpcProviderDirectory directory,
            RpcConsumerChannelFactory channelFactory,
            EgonRpcProperties properties) {
        if (directory == null
                || channelFactory == null
                || properties == null) {
            throw new IllegalArgumentException(
                    "RPC Provider Directory, Channel Factory and properties are required"
            );
        }
        this.directory = directory;
        this.channelFactory = channelFactory;
        this.properties = properties.getConsumer();
        validateProperties();
    }

    public ProviderRpcInvocationChannelProvider register(
            RpcProviderQuery query) {
        Objects.requireNonNull(query, "RPC Provider query is required");
        synchronized (monitor) {
            Registration registration = registrations.computeIfAbsent(
                    query,
                    ignored -> new Registration()
            );
            if (running && registration.subscription == null) {
                subscribe(query, registration);
            }
        }
        return new ProviderRpcInvocationChannelProvider(this, query);
    }

    @Override
    public void start() {
        synchronized (monitor) {
            if (running) {
                return;
            }
            validateProperties();
            running = true;
            drainExecutor = newDrainExecutor();
            try {
                registrations.forEach(this::subscribe);
                drainExecutor.scheduleWithFixedDelay(
                        this::expireProviders,
                        100,
                        100,
                        TimeUnit.MILLISECONDS
                );
            } catch (RuntimeException exception) {
                cleanupStartup();
                throw new EgonRpcException(
                        EgonRpcErrorCode.RPC_PROVIDER_UNAVAILABLE,
                        "RPC Provider discovery failed",
                        exception
                );
            }
        }
    }

    @Override
    public void stop() {
        ScheduledExecutorService executor;
        synchronized (monitor) {
            running = false;
            registrations.values().forEach(this::closeSubscription);
            registrations.values().forEach(registration -> {
                registration.activeProviders.forEach(this::closeNow);
                registration.activeProviders = List.of();
                registration.revision = -1;
                registration.roundRobinSequence.set(0);
            });
            drainingChannels.forEach(ManagedChannel::shutdownNow);
            drainingChannels.clear();
            executor = drainExecutor;
            drainExecutor = null;
        }
        shutdownExecutor(executor);
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 49;
    }

    ManagedChannel currentChannel(
            RpcProviderQuery query,
            Set<ManagedChannel> excluded) {
        expireProviders();
        synchronized (monitor) {
            Registration registration = registrations.get(query);
            Set<ManagedChannel> attempted = excluded == null
                    ? Set.of() : excluded;
            List<ActiveProvider> candidates = registration == null
                    ? List.of()
                    : registration.activeProviders.stream()
                    .filter(provider -> !attempted.contains(provider.channel()))
                    .filter(provider -> !provider.channel().isShutdown())
                    .toList();
            if (!running || candidates.isEmpty()) {
                throw providerUnavailable(query);
            }
            int index = Math.floorMod(
                    registration.roundRobinSequence.getAndIncrement(),
                    candidates.size()
            );
            return candidates.get(index).channel();
        }
    }

    void recordFailure(
            RpcProviderQuery query,
            ManagedChannel failed) {
        if (failed == null) {
            return;
        }
        synchronized (monitor) {
            Registration registration = registrations.get(query);
            if (registration == null) {
                return;
            }
            List<ActiveProvider> remaining = new ArrayList<>();
            for (ActiveProvider provider : registration.activeProviders) {
                if (provider.channel() == failed) {
                    drain(provider);
                } else {
                    remaining.add(provider);
                }
            }
            registration.activeProviders = List.copyOf(remaining);
        }
    }

    private void subscribe(
            RpcProviderQuery query,
            Registration registration) {
        if (registration.subscription != null) {
            return;
        }
        registration.subscription = directory.subscribe(
                query,
                snapshot -> acceptSnapshot(query, snapshot)
        );
    }

    private void acceptSnapshot(
            RpcProviderQuery query,
            RpcProviderSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "RPC Provider snapshot is required");
        synchronized (monitor) {
            if (!running) {
                return;
            }
            Registration registration = registrations.get(query);
            if (registration == null
                    || snapshot.revision() < registration.revision) {
                return;
            }
            registration.revision = snapshot.revision();
            Instant now = Instant.now();
            Map<ProviderIdentity, RpcProviderEndpoint> desired =
                    new LinkedHashMap<>();
            snapshot.endpoints().stream()
                    .filter(Objects::nonNull)
                    .filter(endpoint -> endpoint.activeAt(now))
                    .sorted(Comparator
                            .comparing(RpcProviderEndpoint::instanceId)
                            .thenComparing(RpcProviderEndpoint::leaseId))
                    .forEach(endpoint -> desired.putIfAbsent(
                            ProviderIdentity.from(endpoint),
                            endpoint
                    ));
            reconcile(registration, desired);
        }
    }

    private void reconcile(
            Registration registration,
            Map<ProviderIdentity, RpcProviderEndpoint> desired) {
        Map<ProviderIdentity, ActiveProvider> existing =
                new LinkedHashMap<>();
        registration.activeProviders.forEach(provider -> existing.put(
                ProviderIdentity.from(provider.endpoint()),
                provider
        ));
        List<ActiveProvider> next = new ArrayList<>();
        desired.forEach((identity, endpoint) -> {
            ActiveProvider retained = existing.remove(identity);
            if (retained != null && !retained.channel().isShutdown()) {
                next.add(new ActiveProvider(endpoint, retained.channel()));
                return;
            }
            ActiveProvider connected = connect(endpoint);
            if (connected != null) {
                next.add(connected);
            }
        });
        existing.values().forEach(this::drain);
        registration.activeProviders = List.copyOf(next);
    }

    private ActiveProvider connect(RpcProviderEndpoint endpoint) {
        ManagedChannel channel = channelFactory.create(endpoint);
        if (!channelFactory.awaitReady(
                channel,
                properties.getDefaultTimeoutMs()
        )) {
            channel.shutdownNow();
            return null;
        }
        return new ActiveProvider(endpoint, channel);
    }

    private void expireProviders() {
        synchronized (monitor) {
            Instant now = Instant.now();
            registrations.values().forEach(registration -> {
                List<ActiveProvider> retained = new ArrayList<>();
                for (ActiveProvider provider
                        : registration.activeProviders) {
                    if (provider.endpoint().activeAt(now)
                            && !provider.channel().isShutdown()) {
                        retained.add(provider);
                    } else {
                        drain(provider);
                    }
                }
                if (retained.size()
                        != registration.activeProviders.size()) {
                    registration.activeProviders = List.copyOf(retained);
                }
            });
        }
    }

    private void drain(ActiveProvider provider) {
        if (provider == null) {
            return;
        }
        ManagedChannel channel = provider.channel();
        ScheduledExecutorService executor = drainExecutor;
        if (executor == null) {
            channel.shutdownNow();
            return;
        }
        drainingChannels.add(channel);
        channel.shutdown();
        executor.schedule(
                () -> {
                    synchronized (monitor) {
                        drainingChannels.remove(channel);
                    }
                    channel.shutdownNow();
                },
                properties.getChannelDrainTimeoutMs(),
                TimeUnit.MILLISECONDS
        );
    }

    private void closeSubscription(Registration registration) {
        if (registration.subscription != null) {
            registration.subscription.close();
            registration.subscription = null;
        }
    }

    private void closeNow(ActiveProvider provider) {
        if (provider != null) {
            provider.channel().shutdownNow();
        }
    }

    private void cleanupStartup() {
        registrations.values().forEach(this::closeSubscription);
        registrations.values().forEach(registration -> {
            registration.activeProviders.forEach(this::closeNow);
            registration.activeProviders = List.of();
        });
        drainingChannels.forEach(ManagedChannel::shutdownNow);
        drainingChannels.clear();
        running = false;
        ScheduledExecutorService executor = drainExecutor;
        drainExecutor = null;
        shutdownExecutor(executor);
    }

    private void validateProperties() {
        if (properties.getDefaultTimeoutMs() <= 0
                || properties.getChannelDrainTimeoutMs() <= 0) {
            throw new EgonRpcException(
                    EgonRpcErrorCode.RPC_INVALID_CONTRACT,
                    "RPC Consumer timeout settings must be positive"
            );
        }
    }

    private EgonRpcException providerUnavailable(RpcProviderQuery query) {
        return new EgonRpcException(
                EgonRpcErrorCode.RPC_PROVIDER_UNAVAILABLE,
                "RPC Provider is unavailable for " + query.serviceName()
        );
    }

    private ScheduledExecutorService newDrainExecutor() {
        return Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(
                    runnable,
                    "egon-rpc-consumer-provider-channel-drain"
            );
            thread.setDaemon(true);
            return thread;
        });
    }

    private void shutdownExecutor(ScheduledExecutorService executor) {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private static final class Registration {

        private final AtomicLong roundRobinSequence = new AtomicLong();

        private long revision = -1;

        private List<ActiveProvider> activeProviders = List.of();

        private RpcProviderSubscription subscription;
    }

    private record ActiveProvider(
            RpcProviderEndpoint endpoint,
            ManagedChannel channel
    ) {
    }

    private record ProviderIdentity(
            String instanceId,
            String leaseId,
            String host,
            int port,
            boolean secure
    ) {

        private static ProviderIdentity from(RpcProviderEndpoint endpoint) {
            return new ProviderIdentity(
                    endpoint.instanceId(),
                    endpoint.leaseId(),
                    endpoint.host(),
                    endpoint.port(),
                    endpoint.secure()
            );
        }
    }
}
