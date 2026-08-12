package top.egon.cola.component.gateway.engine.observability;

import top.egon.cola.component.gateway.contract.observability.GatewayCallEventV1;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A best-effort dispatcher with drop-new backpressure semantics.
 * 补充说明 / Supplementary summary: {@code GatewayCallEventDispatcher} 是分发器，位于当前 Gateway 模块的相关包中，负责网关调用事件分发器相关的职责与边界。
 * English supplement: {@code GatewayCallEventDispatcher} is a gateway call event dispatcher dispatcher in the current Gateway module; it owns the gateway call event dispatcher-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayCallEventDispatcher
        implements GatewayCallCompletionListener, AutoCloseable {

    /**
     * 中文说明：保存 queue 对应的状态、依赖或配置值；字段类型为 {@code ArrayBlockingQueue<QueuedEvent>}，由 {@code GatewayCallEventDispatcher} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by queue; its type is {@code ArrayBlockingQueue<QueuedEvent>}, and {@code GatewayCallEventDispatcher} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallEventDispatcher} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventDispatcher}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ArrayBlockingQueue<QueuedEvent> queue;

    /**
     * 中文说明：保存 maxQueuedBytes 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayCallEventDispatcher} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by max queued bytes; its type is {@code long}, and {@code GatewayCallEventDispatcher} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallEventDispatcher} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventDispatcher}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final long maxQueuedBytes;

    /**
     * 中文说明：保存 serializer 对应的状态、依赖或配置值；字段类型为 {@code GatewayCallEventSerializer}，由 {@code GatewayCallEventDispatcher} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by serializer; its type is {@code GatewayCallEventSerializer}, and {@code GatewayCallEventDispatcher} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallEventDispatcher} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventDispatcher}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayCallEventSerializer serializer;

    /**
     * 中文说明：保存 sink 对应的状态、依赖或配置值；字段类型为 {@code GatewayCallEventSink}，由 {@code GatewayCallEventDispatcher} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by sink; its type is {@code GatewayCallEventSink}, and {@code GatewayCallEventDispatcher} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallEventDispatcher} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventDispatcher}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayCallEventSink sink;

    /**
     * 中文说明：保存 shutdownDrain 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayCallEventDispatcher} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by shutdown drain; its type is {@code Duration}, and {@code GatewayCallEventDispatcher} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallEventDispatcher} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventDispatcher}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Duration shutdownDrain;

    /**
     * 中文说明：保存 queuedBytes 对应的状态、依赖或配置值；字段类型为 {@code AtomicLong}，由 {@code GatewayCallEventDispatcher} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by queued bytes; its type is {@code AtomicLong}, and {@code GatewayCallEventDispatcher} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallEventDispatcher} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventDispatcher}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AtomicLong queuedBytes = new AtomicLong();

    /**
     * 中文说明：保存 accepted 对应的状态、依赖或配置值；字段类型为 {@code AtomicLong}，由 {@code GatewayCallEventDispatcher} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by accepted; its type is {@code AtomicLong}, and {@code GatewayCallEventDispatcher} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallEventDispatcher} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventDispatcher}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AtomicLong accepted = new AtomicLong();

    /**
     * 中文说明：保存 sent 对应的状态、依赖或配置值；字段类型为 {@code AtomicLong}，由 {@code GatewayCallEventDispatcher} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by sent; its type is {@code AtomicLong}, and {@code GatewayCallEventDispatcher} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallEventDispatcher} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventDispatcher}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AtomicLong sent = new AtomicLong();

    /**
     * 中文说明：保存 droppedQueueFull 对应的状态、依赖或配置值；字段类型为 {@code AtomicLong}，由 {@code GatewayCallEventDispatcher} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by dropped queue full; its type is {@code AtomicLong}, and {@code GatewayCallEventDispatcher} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallEventDispatcher} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventDispatcher}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AtomicLong droppedQueueFull = new AtomicLong();

    /**
     * 中文说明：保存 droppedPayloadTooLarge 对应的状态、依赖或配置值；字段类型为 {@code AtomicLong}，由 {@code GatewayCallEventDispatcher} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by dropped payload too large; its type is {@code AtomicLong}, and {@code GatewayCallEventDispatcher} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallEventDispatcher} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventDispatcher}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AtomicLong droppedPayloadTooLarge = new AtomicLong();

    /**
     * 中文说明：保存 failed 对应的状态、依赖或配置值；字段类型为 {@code AtomicLong}，由 {@code GatewayCallEventDispatcher} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by failed; its type is {@code AtomicLong}, and {@code GatewayCallEventDispatcher} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallEventDispatcher} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventDispatcher}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AtomicLong failed = new AtomicLong();

    /**
     * 中文说明：保存 worker 对应的状态、依赖或配置值；字段类型为 {@code Thread}，由 {@code GatewayCallEventDispatcher} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by worker; its type is {@code Thread}, and {@code GatewayCallEventDispatcher} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallEventDispatcher} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventDispatcher}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Thread worker;

    /**
     * 中文说明：保存 accepting 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayCallEventDispatcher} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by accepting; its type is {@code boolean}, and {@code GatewayCallEventDispatcher} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallEventDispatcher} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventDispatcher}; do not couple callers to its representation when the owning type exposes an API.
     */
    private volatile boolean accepting = true;

    /**
     * 中文说明：创建 {@code GatewayCallEventDispatcher} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayCallEventDispatcher} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param maxQueuedEvents 参数 maxQueuedEvents；parameter max queued events。
     * @param maxQueuedBytes 参数 maxQueuedBytes；parameter max queued bytes。
     * @param shutdownDrain 参数 shutdownDrain；parameter shutdown drain。
     * @param serializer 参数 serializer；parameter serializer。
     * @param sink 参数 sink；parameter sink。
     */
    public GatewayCallEventDispatcher(
            int maxQueuedEvents,
            long maxQueuedBytes,
            Duration shutdownDrain,
            GatewayCallEventSerializer serializer,
            GatewayCallEventSink sink) {
        if (maxQueuedEvents < 1 || maxQueuedBytes < 1) {
            throw new IllegalArgumentException(
                    "queue bounds must be positive"
            );
        }
        this.queue = new ArrayBlockingQueue<>(maxQueuedEvents);
        this.maxQueuedBytes = maxQueuedBytes;
        this.shutdownDrain = Objects.requireNonNull(
                shutdownDrain,
                "shutdownDrain"
        );
        this.serializer = Objects.requireNonNull(serializer, "serializer");
        this.sink = Objects.requireNonNull(sink, "sink");
        worker = Thread.ofPlatform()
                .daemon(true)
                .name("gateway-call-event-dispatcher")
                .unstarted(this::run);
        worker.start();
    }

    /**
     * 中文说明：执行 onComplete 操作；该方法是 {@code GatewayCallEventDispatcher} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the on complete operation; this method is the invocation entry point on {@code GatewayCallEventDispatcher} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallEventDispatcher.onComplete(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param event 参数 事件；parameter event。
     */
    @Override
    public void onComplete(GatewayCallEventV1 event) {
        if (!accepting) {
            droppedQueueFull.incrementAndGet();
            return;
        }
        long estimatedBytes = estimateBytes(event);
        synchronized (queue) {
            if (!accepting
                    || queue.remainingCapacity() == 0
                    || queuedBytes.get() + estimatedBytes
                    > maxQueuedBytes
                    || !queue.offer(new QueuedEvent(
                    event,
                    estimatedBytes
            ))) {
                droppedQueueFull.incrementAndGet();
                return;
            }
            queuedBytes.addAndGet(estimatedBytes);
            accepted.incrementAndGet();
        }
    }

    /**
     * 中文说明：执行 健康 操作；该方法是 {@code GatewayCallEventDispatcher} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the health operation; this method is the invocation entry point on {@code GatewayCallEventDispatcher} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallEventDispatcher.health(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 健康 的处理结果；returns the result of the operation.
     */
    public Health health() {
        return new Health(
                accepting,
                queue.size(),
                queuedBytes.get(),
                accepted.get(),
                sent.get(),
                droppedQueueFull.get(),
                droppedPayloadTooLarge.get(),
                failed.get()
        );
    }

    /**
     * 中文说明：执行 close 操作；该方法是 {@code GatewayCallEventDispatcher} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the close operation; this method is the invocation entry point on {@code GatewayCallEventDispatcher} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallEventDispatcher.close(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    @Override
    public void close() {
        accepting = false;
        long deadline = System.nanoTime() + shutdownDrain.toNanos();
        while (!queue.isEmpty() && System.nanoTime() < deadline) {
            try {
                worker.join(Math.min(
                        100,
                        Math.max(
                                1,
                                TimeUnit.NANOSECONDS.toMillis(
                                        deadline - System.nanoTime()
                                )
                        )
                ));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        worker.interrupt();
        try {
            worker.join(500);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } finally {
            sink.close();
        }
    }

    /**
     * 中文说明：执行 run 操作；该方法是 {@code GatewayCallEventDispatcher} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the run operation; this method is the invocation entry point on {@code GatewayCallEventDispatcher} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallEventDispatcher.run(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    private void run() {
        while (accepting || !queue.isEmpty()) {
            try {
                QueuedEvent queued = queue.poll(200, TimeUnit.MILLISECONDS);
                if (queued != null) {
                    queuedBytes.addAndGet(-queued.estimatedBytes);
                    dispatch(queued.event);
                }
            } catch (InterruptedException interrupted) {
                if (accepting) {
                    Thread.currentThread().interrupt();
                }
                return;
            }
        }
    }

    /**
     * 中文说明：执行 dispatch 操作；该方法是 {@code GatewayCallEventDispatcher} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the dispatch operation; this method is the invocation entry point on {@code GatewayCallEventDispatcher} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallEventDispatcher.dispatch(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param event 参数 事件；parameter event。
     */
    private void dispatch(GatewayCallEventV1 event) {
        try {
            byte[] payload = serializer.serialize(event);
            sink.send(event, payload);
            sent.incrementAndGet();
        } catch (GatewayCallEventSerializer.PayloadTooLargeException tooLarge) {
            droppedPayloadTooLarge.incrementAndGet();
        } catch (RuntimeException failure) {
            failed.incrementAndGet();
        }
    }

    /**
     * 中文说明：执行 estimateBytes 操作；该方法是 {@code GatewayCallEventDispatcher} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the estimate bytes operation; this method is the invocation entry point on {@code GatewayCallEventDispatcher} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallEventDispatcher.estimateBytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param event 参数 事件；parameter event。
     * @return 返回 estimateBytes 的处理结果；returns the result of the operation.
     */
    private static long estimateBytes(GatewayCallEventV1 event) {
        return Math.min(
                GatewayCallEventSerializer.MAX_PAYLOAD_BYTES,
                2048L + event.attempts().size() * 256L
        );
    }

    /**
     * 中文说明：{@code QueuedEvent} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Queued事件相关的职责与边界。
     * English summary: {@code QueuedEvent} is an immutable data carrier in the current Gateway module; it owns the queued event-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param event 参数 事件；parameter event。
     * @param estimatedBytes 参数 estimatedBytes；parameter estimated bytes。
     */
    private record QueuedEvent(
            /**
             * 中文说明：保存 事件 对应的状态、依赖或配置值；字段类型为 {@code GatewayCallEventV1}，由 {@code GatewayCallEventDispatcher.QueuedEvent} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by event; its type is {@code GatewayCallEventV1}, and {@code GatewayCallEventDispatcher.QueuedEvent} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCallEventDispatcher.QueuedEvent} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventDispatcher.QueuedEvent}; do not couple callers to its representation when the owning type exposes an API.
             */
            GatewayCallEventV1 event,
            /**
             * 中文说明：保存 estimatedBytes 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayCallEventDispatcher.QueuedEvent} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by estimated bytes; its type is {@code long}, and {@code GatewayCallEventDispatcher.QueuedEvent} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCallEventDispatcher.QueuedEvent} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventDispatcher.QueuedEvent}; do not couple callers to its representation when the owning type exposes an API.
             */
            long estimatedBytes
    ) {
    }

    /**
     * 中文说明：{@code Health} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责健康相关的职责与边界。
     * English summary: {@code Health} is an immutable data carrier in the current Gateway module; it owns the health-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param accepting 参数 accepting；parameter accepting。
     * @param queuedEvents 参数 queuedEvents；parameter queued events。
     * @param queuedBytes 参数 queuedBytes；parameter queued bytes。
     * @param accepted 参数 accepted；parameter accepted。
     * @param sent 参数 sent；parameter sent。
     * @param droppedQueueFull 参数 droppedQueueFull；parameter dropped queue full。
     * @param droppedPayloadTooLarge 参数 droppedPayloadTooLarge；parameter dropped payload too large。
     * @param failed 参数 failed；parameter failed。
     */
    public record Health(
            /**
             * 中文说明：保存 accepting 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayCallEventDispatcher.Health} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by accepting; its type is {@code boolean}, and {@code GatewayCallEventDispatcher.Health} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCallEventDispatcher.Health} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventDispatcher.Health}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean accepting,
            /**
             * 中文说明：保存 queuedEvents 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayCallEventDispatcher.Health} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by queued events; its type is {@code int}, and {@code GatewayCallEventDispatcher.Health} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCallEventDispatcher.Health} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventDispatcher.Health}; do not couple callers to its representation when the owning type exposes an API.
             */
            int queuedEvents,
            /**
             * 中文说明：保存 queuedBytes 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayCallEventDispatcher.Health} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by queued bytes; its type is {@code long}, and {@code GatewayCallEventDispatcher.Health} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCallEventDispatcher.Health} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventDispatcher.Health}; do not couple callers to its representation when the owning type exposes an API.
             */
            long queuedBytes,
            /**
             * 中文说明：保存 accepted 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayCallEventDispatcher.Health} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by accepted; its type is {@code long}, and {@code GatewayCallEventDispatcher.Health} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCallEventDispatcher.Health} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventDispatcher.Health}; do not couple callers to its representation when the owning type exposes an API.
             */
            long accepted,
            /**
             * 中文说明：保存 sent 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayCallEventDispatcher.Health} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by sent; its type is {@code long}, and {@code GatewayCallEventDispatcher.Health} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCallEventDispatcher.Health} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventDispatcher.Health}; do not couple callers to its representation when the owning type exposes an API.
             */
            long sent,
            /**
             * 中文说明：保存 droppedQueueFull 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayCallEventDispatcher.Health} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by dropped queue full; its type is {@code long}, and {@code GatewayCallEventDispatcher.Health} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCallEventDispatcher.Health} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventDispatcher.Health}; do not couple callers to its representation when the owning type exposes an API.
             */
            long droppedQueueFull,
            /**
             * 中文说明：保存 droppedPayloadTooLarge 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayCallEventDispatcher.Health} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by dropped payload too large; its type is {@code long}, and {@code GatewayCallEventDispatcher.Health} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCallEventDispatcher.Health} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventDispatcher.Health}; do not couple callers to its representation when the owning type exposes an API.
             */
            long droppedPayloadTooLarge,
            /**
             * 中文说明：保存 failed 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayCallEventDispatcher.Health} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by failed; its type is {@code long}, and {@code GatewayCallEventDispatcher.Health} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCallEventDispatcher.Health} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventDispatcher.Health}; do not couple callers to its representation when the owning type exposes an API.
             */
            long failed
    ) {
    }
}
