package top.egon.cola.component.gateway.engine.mcp.service;

import org.reactivestreams.Publisher;
import org.springframework.context.SmartLifecycle;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.mcp.task.service.McpTaskExecutor;
import top.egon.cola.component.gateway.mcp.task.service.McpTaskService;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Polls the shared task store; lease ownership keeps nodes mutually exclusive.
 * 补充说明 / Supplementary summary: {@code McpTaskWorker} 是类型，位于当前 Gateway 模块的相关包中，负责MCP任务Worker相关的职责与边界。
 * English supplement: {@code McpTaskWorker} is a type in the current Gateway module; it owns the mcp task worker-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpTaskWorker implements SmartLifecycle {

    /**
     * 中文说明：保存 tasks 对应的状态、依赖或配置值；字段类型为 {@code McpTaskService}，由 {@code McpTaskWorker} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by tasks; its type is {@code McpTaskService}, and {@code McpTaskWorker} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpTaskWorker} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskWorker}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpTaskService tasks;

    /**
     * 中文说明：保存 executor 对应的状态、依赖或配置值；字段类型为 {@code McpTaskExecutor}，由 {@code McpTaskWorker} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by executor; its type is {@code McpTaskExecutor}, and {@code McpTaskWorker} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpTaskWorker} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskWorker}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpTaskExecutor executor;

    /**
     * 中文说明：保存 workerOwner 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpTaskWorker} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by worker owner; its type is {@code String}, and {@code McpTaskWorker} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpTaskWorker} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskWorker}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String workerOwner;

    /**
     * 中文说明：保存 租约Duration 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code McpTaskWorker} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by lease duration; its type is {@code Duration}, and {@code McpTaskWorker} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpTaskWorker} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskWorker}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Duration leaseDuration;

    /**
     * 中文说明：保存 pollInterval 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code McpTaskWorker} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by poll interval; its type is {@code Duration}, and {@code McpTaskWorker} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpTaskWorker} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskWorker}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Duration pollInterval;

    /**
     * 中文说明：保存 running 对应的状态、依赖或配置值；字段类型为 {@code AtomicBoolean}，由 {@code McpTaskWorker} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by running; its type is {@code AtomicBoolean}, and {@code McpTaskWorker} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpTaskWorker} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskWorker}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AtomicBoolean running = new AtomicBoolean();

    /**
     * 中文说明：保存 scheduler 对应的状态、依赖或配置值；字段类型为 {@code ScheduledExecutorService}，由 {@code McpTaskWorker} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by scheduler; its type is {@code ScheduledExecutorService}, and {@code McpTaskWorker} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpTaskWorker} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskWorker}; do not couple callers to its representation when the owning type exposes an API.
     */
    private ScheduledExecutorService scheduler;

    /**
     * 中文说明：创建 {@code McpTaskWorker} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpTaskWorker} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param tasks 参数 tasks；parameter tasks。
     * @param executor 参数 executor；parameter executor。
     * @param workerOwner 参数 workerOwner；parameter worker owner。
     * @param leaseDuration 参数 租约Duration；parameter lease duration。
     * @param pollInterval 参数 pollInterval；parameter poll interval。
     */
    public McpTaskWorker(
            McpTaskService tasks,
            McpTaskExecutor executor,
            String workerOwner,
            Duration leaseDuration,
            Duration pollInterval) {
        this.tasks = Objects.requireNonNull(tasks, "tasks");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.workerOwner = required(workerOwner, "workerOwner");
        this.leaseDuration = positive(leaseDuration, "leaseDuration");
        this.pollInterval = positive(pollInterval, "pollInterval");
    }

    /**
     * 中文说明：执行 runOnce 操作；该方法是 {@code McpTaskWorker} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the run once operation; this method is the invocation entry point on {@code McpTaskWorker} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskWorker.runOnce(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 runOnce 的处理结果；returns the result of the operation.
     */
    public Publisher<Void> runOnce() {
        return tasks.executeNext(workerOwner, executor);
    }

    /**
     * 中文说明：执行 start 操作；该方法是 {@code McpTaskWorker} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the start operation; this method is the invocation entry point on {@code McpTaskWorker} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskWorker.start(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "gateway-mcp-task-worker");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(
                this::pollSafely,
                0L,
                pollInterval.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * 中文说明：执行 stop 操作；该方法是 {@code McpTaskWorker} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the stop operation; this method is the invocation entry point on {@code McpTaskWorker} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskWorker.stop(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    @Override
    public void stop() {
        running.set(false);
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    /**
     * 中文说明：执行 isRunning 操作；该方法是 {@code McpTaskWorker} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the is running operation; this method is the invocation entry point on {@code McpTaskWorker} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskWorker.isRunning(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 isRunning 的处理结果；returns the result of the operation.
     */
    @Override
    public boolean isRunning() {
        return running.get();
    }

    /**
     * 中文说明：执行 pollSafely 操作；该方法是 {@code McpTaskWorker} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the poll safely operation; this method is the invocation entry point on {@code McpTaskWorker} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskWorker.pollSafely(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    private void pollSafely() {
        if (!running.get()) {
            return;
        }
        Mono.from(runOnce())
                .then(Mono.from(tasks.cleanup()))
                .onErrorComplete()
                .block();
    }

    /**
     * 中文说明：执行 positive 操作；该方法是 {@code McpTaskWorker} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the positive operation; this method is the invocation entry point on {@code McpTaskWorker} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskWorker.positive(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 positive 的处理结果；returns the result of the operation.
     */
    private Duration positive(Duration value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code McpTaskWorker} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code McpTaskWorker} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskWorker.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
