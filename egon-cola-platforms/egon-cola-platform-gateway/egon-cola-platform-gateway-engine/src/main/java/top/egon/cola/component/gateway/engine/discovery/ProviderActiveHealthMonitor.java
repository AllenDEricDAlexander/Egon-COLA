package top.egon.cola.component.gateway.engine.discovery;

import org.springframework.context.SmartLifecycle;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.provider.ProviderRegistryState;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ProviderActiveHealthMonitor
        implements SmartLifecycle, AutoCloseable {

    private final ProviderDirectory directory;

    private final Map<ProviderProtocolType, ProviderActiveHealthProbe> probes;

    private final ActiveHealthTracker tracker;

    private final ActiveHealthProbePolicy policy;

    private final Scheduler probeScheduler;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(
                        runnable,
                        "gateway-active-health-scheduler"
                );
                thread.setDaemon(true);
                return thread;
            });

    private final AtomicBoolean running = new AtomicBoolean();

    private volatile Disposable inFlight;

    public ProviderActiveHealthMonitor(
            ProviderDirectory directory,
            Map<ProviderProtocolType, ProviderActiveHealthProbe> probes,
            ActiveHealthTracker tracker,
            ActiveHealthProbePolicy policy) {
        this.directory = Objects.requireNonNull(directory, "directory");
        this.probes = Map.copyOf(Objects.requireNonNull(probes, "probes"));
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.policy = Objects.requireNonNull(policy, "policy");
        probeScheduler = Schedulers.newBoundedElastic(
                policy.maximumConcurrency(),
                Math.max(100, policy.maximumConcurrency() * 100),
                "gateway-active-health-probe"
        );
    }

    public Mono<Void> probeOnce() {
        if (!policy.enabled()) {
            return Mono.empty();
        }
        return Flux.fromIterable(directory.snapshot().values())
                .flatMapIterable(snapshot -> snapshot.instances())
                .filter(instance -> instance.registryState()
                        == ProviderRegistryState.REGISTERED)
                .flatMap(
                        this::probe,
                        policy.maximumConcurrency()
                )
                .then();
    }

    private Mono<Void> probe(ProviderInstance instance) {
        ProviderActiveHealthProbe probe = probes.get(
                instance.serviceKey().protocolType()
        );
        if (probe == null) {
            return Mono.empty();
        }
        return probe.probe(instance, policy)
                .subscribeOn(probeScheduler)
                .timeout(policy.timeout())
                .onErrorReturn(false)
                .defaultIfEmpty(false)
                .doOnNext(successful -> tracker.record(
                        instance.runtimeIdentity(),
                        successful
                ))
                .then();
    }

    @Override
    public void start() {
        if (!policy.enabled() || !running.compareAndSet(false, true)) {
            return;
        }
        schedule(Duration.ZERO);
    }

    private void schedule(Duration delay) {
        if (!running.get()) {
            return;
        }
        scheduler.schedule(
                () -> {
                    if (!running.get()) {
                        return;
                    }
                    inFlight = probeOnce().doFinally(
                            signal -> schedule(nextDelay())
                    ).subscribe();
                },
                delay.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    private Duration nextDelay() {
        if (policy.jitterRatio() == 0) {
            return policy.interval();
        }
        double offset = ThreadLocalRandom.current().nextDouble(
                -policy.jitterRatio(),
                policy.jitterRatio()
        );
        return Duration.ofMillis(Math.max(
                1,
                Math.round(policy.interval().toMillis() * (1 + offset))
        ));
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            Disposable active = inFlight;
            if (active != null) {
                active.dispose();
            }
        }
        scheduler.shutdownNow();
        probeScheduler.dispose();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 200;
    }

    @Override
    public void close() {
        stop();
    }
}
