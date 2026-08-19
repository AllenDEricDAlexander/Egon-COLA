package top.egon.cola.component.gateway.engine.common.provider.service;

import top.egon.cola.component.gateway.engine.common.provider.domain.ActiveHealthProbePolicy;

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

/**
 * 中文说明：{@code ProviderActiveHealthMonitor} 是类型，位于当前 Gateway 模块的相关包中，负责提供方Active健康监控器相关的职责与边界。
 * English summary: {@code ProviderActiveHealthMonitor} is a type in the current Gateway module; it owns the provider active health monitor-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class ProviderActiveHealthMonitor
        implements SmartLifecycle, AutoCloseable {

    /**
     * 中文说明：保存 directory 对应的状态、依赖或配置值；字段类型为 {@code ProviderDirectory}，由 {@code ProviderActiveHealthMonitor} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by directory; its type is {@code ProviderDirectory}, and {@code ProviderActiveHealthMonitor} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderActiveHealthMonitor} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderActiveHealthMonitor}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ProviderDirectory directory;

    /**
     * 中文说明：保存 probes 对应的状态、依赖或配置值；字段类型为 {@code Map<ProviderProtocolType, ProviderActiveHealthProbe>}，由 {@code ProviderActiveHealthMonitor} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by probes; its type is {@code Map<ProviderProtocolType, ProviderActiveHealthProbe>}, and {@code ProviderActiveHealthMonitor} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderActiveHealthMonitor} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderActiveHealthMonitor}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Map<ProviderProtocolType, ProviderActiveHealthProbe> probes;

    /**
     * 中文说明：保存 tracker 对应的状态、依赖或配置值；字段类型为 {@code ActiveHealthTracker}，由 {@code ProviderActiveHealthMonitor} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by tracker; its type is {@code ActiveHealthTracker}, and {@code ProviderActiveHealthMonitor} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderActiveHealthMonitor} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderActiveHealthMonitor}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ActiveHealthTracker tracker;

    /**
     * 中文说明：保存 策略 对应的状态、依赖或配置值；字段类型为 {@code ActiveHealthProbePolicy}，由 {@code ProviderActiveHealthMonitor} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by policy; its type is {@code ActiveHealthProbePolicy}, and {@code ProviderActiveHealthMonitor} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderActiveHealthMonitor} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderActiveHealthMonitor}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ActiveHealthProbePolicy policy;

    /**
     * 中文说明：保存 probeScheduler 对应的状态、依赖或配置值；字段类型为 {@code Scheduler}，由 {@code ProviderActiveHealthMonitor} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by probe scheduler; its type is {@code Scheduler}, and {@code ProviderActiveHealthMonitor} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderActiveHealthMonitor} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderActiveHealthMonitor}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Scheduler probeScheduler;

    /**
     * 中文说明：保存 scheduler 对应的状态、依赖或配置值；字段类型为 {@code ScheduledExecutorService}，由 {@code ProviderActiveHealthMonitor} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by scheduler; its type is {@code ScheduledExecutorService}, and {@code ProviderActiveHealthMonitor} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderActiveHealthMonitor} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderActiveHealthMonitor}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(
                        runnable,
                        "gateway-active-health-scheduler"
                );
                thread.setDaemon(true);
                return thread;
            });

    /**
     * 中文说明：保存 running 对应的状态、依赖或配置值；字段类型为 {@code AtomicBoolean}，由 {@code ProviderActiveHealthMonitor} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by running; its type is {@code AtomicBoolean}, and {@code ProviderActiveHealthMonitor} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderActiveHealthMonitor} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderActiveHealthMonitor}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AtomicBoolean running = new AtomicBoolean();

    /**
     * 中文说明：保存 inFlight 对应的状态、依赖或配置值；字段类型为 {@code Disposable}，由 {@code ProviderActiveHealthMonitor} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by in flight; its type is {@code Disposable}, and {@code ProviderActiveHealthMonitor} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderActiveHealthMonitor} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderActiveHealthMonitor}; do not couple callers to its representation when the owning type exposes an API.
     */
    private volatile Disposable inFlight;

    /**
     * 中文说明：创建 {@code ProviderActiveHealthMonitor} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code ProviderActiveHealthMonitor} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param directory 参数 directory；parameter directory。
     * @param probes 参数 probes；parameter probes。
     * @param tracker 参数 tracker；parameter tracker。
     * @param policy 参数 策略；parameter policy。
     */
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

    /**
     * 中文说明：执行 probeOnce 操作；该方法是 {@code ProviderActiveHealthMonitor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the probe once operation; this method is the invocation entry point on {@code ProviderActiveHealthMonitor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderActiveHealthMonitor.probeOnce(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 probeOnce 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 probe 操作；该方法是 {@code ProviderActiveHealthMonitor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the probe operation; this method is the invocation entry point on {@code ProviderActiveHealthMonitor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderActiveHealthMonitor.probe(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param instance 参数 instance；parameter instance。
     * @return 返回 probe 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 start 操作；该方法是 {@code ProviderActiveHealthMonitor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the start operation; this method is the invocation entry point on {@code ProviderActiveHealthMonitor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderActiveHealthMonitor.start(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    @Override
    public void start() {
        if (!policy.enabled() || !running.compareAndSet(false, true)) {
            return;
        }
        schedule(Duration.ZERO);
    }

    /**
     * 中文说明：执行 schedule 操作；该方法是 {@code ProviderActiveHealthMonitor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the schedule operation; this method is the invocation entry point on {@code ProviderActiveHealthMonitor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderActiveHealthMonitor.schedule(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param delay 参数 delay；parameter delay。
     */
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

    /**
     * 中文说明：执行 nextDelay 操作；该方法是 {@code ProviderActiveHealthMonitor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the next delay operation; this method is the invocation entry point on {@code ProviderActiveHealthMonitor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderActiveHealthMonitor.nextDelay(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 nextDelay 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 stop 操作；该方法是 {@code ProviderActiveHealthMonitor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the stop operation; this method is the invocation entry point on {@code ProviderActiveHealthMonitor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderActiveHealthMonitor.stop(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
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

    /**
     * 中文说明：执行 isRunning 操作；该方法是 {@code ProviderActiveHealthMonitor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the is running operation; this method is the invocation entry point on {@code ProviderActiveHealthMonitor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderActiveHealthMonitor.isRunning(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 isRunning 的处理结果；returns the result of the operation.
     */
    @Override
    public boolean isRunning() {
        return running.get();
    }

    /**
     * 中文说明：执行 getPhase 操作；该方法是 {@code ProviderActiveHealthMonitor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get phase operation; this method is the invocation entry point on {@code ProviderActiveHealthMonitor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderActiveHealthMonitor.getPhase(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getPhase 的处理结果；returns the result of the operation.
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 200;
    }

    /**
     * 中文说明：执行 close 操作；该方法是 {@code ProviderActiveHealthMonitor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the close operation; this method is the invocation entry point on {@code ProviderActiveHealthMonitor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderActiveHealthMonitor.close(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    @Override
    public void close() {
        stop();
    }
}
