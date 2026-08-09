package top.egon.cola.component.ddc.configuration.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.ddc.configuration.client.DdcConfigClient;
import top.egon.cola.component.ddc.configuration.model.DdcAckRequest;
import top.egon.cola.component.ddc.observability.DdcTraceSupport;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 通过单线程有界队列异步投递 DDC 发布确认，并对瞬时故障进行指数退避重试。
 * Asynchronously delivers DDC publication acknowledgments through a bounded single-thread queue with
 * exponential-backoff retries for transient failures.
 *
 * <p>同一变化、实例和租约组合在待处理期间会被去重。网络异常和服务端 5xx 响应可重试，其他失败不会重试。</p>
 * <p>The same change, instance, and lease tuple is deduplicated while pending. Network failures and server-side 5xx responses are retryable; other failures are not.</p>
 */
public class DdcAckDelivery implements SmartLifecycle, AutoCloseable {

    /**
     * 当前类的日志记录器。 Logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DdcAckDelivery.class);

    /**
     * ACK 投递工作线程名称。 Name of the ACK delivery worker thread.
     */
    private static final String WORKER_NAME = "egon-cola-ddc-ack-delivery";

    /**
     * 执行 ACK HTTP 调用的管理端客户端。 Administration client performing ACK HTTP calls.
     */
    private final DdcConfigClient adminClient;

    /**
     * 队列、重试和停止参数。 Queue, retry, and shutdown settings.
     */
    private final DdcAckDeliveryProperties properties;

    /**
     * 保护待投递映射和执行器状态检查的监视器。 Monitor protecting pending deliveries and executor-state checks.
     */
    private final Object pendingMonitor = new Object();

    /**
     * 按 ACK 幂等键索引的待投递任务。 Pending deliveries indexed by ACK idempotency key.
     */
    private final Map<AckKey, PendingDelivery> pending = new HashMap<>();

    /**
     * 投递组件是否接受新任务。 Whether the delivery component accepts new tasks.
     */
    private final AtomicBoolean running = new AtomicBoolean();

    /**
     * 成功入队的任务数。 Number of tasks successfully submitted.
     */
    private final AtomicLong submitted = new AtomicLong();

    /**
     * 成功送达的任务数。 Number of tasks delivered successfully.
     */
    private final AtomicLong delivered = new AtomicLong();

    /**
     * 已安排的重试次数。 Number of retries scheduled.
     */
    private final AtomicLong retried = new AtomicLong();

    /**
     * 被待处理任务去重的提交数。 Number of submissions deduplicated against pending tasks.
     */
    private final AtomicLong deduplicated = new AtomicLong();

    /**
     * 因队列达到容量而拒绝的提交数。 Number of submissions rejected because the queue reached capacity.
     */
    private final AtomicLong saturated = new AtomicLong();

    /**
     * 达到最大尝试次数的任务数。 Number of tasks exhausting their maximum attempts.
     */
    private final AtomicLong exhausted = new AtomicLong();

    /**
     * 遭遇不可重试失败的任务数。 Number of tasks encountering non-retryable failures.
     */
    private final AtomicLong nonRetryableFailures = new AtomicLong();

    /**
     * 在组件停止时被拒绝的提交数。 Number of submissions rejected while the component was stopped.
     */
    private final AtomicLong rejectedWhileStopped = new AtomicLong();

    /**
     * 停止时仍待处理并被丢弃的任务数。 Number of pending tasks dropped during shutdown.
     */
    private final AtomicLong droppedOnShutdown = new AtomicLong();

    /**
     * 当前单线程延迟执行器。 Current single-thread scheduled executor.
     */
    private volatile ScheduledThreadPoolExecutor executor;

    /**
     * 最近一个工作执行器是否已终止。 Whether the most recent worker executor has terminated.
     */
    private volatile boolean workerTerminated = true;

    /**
     * 创建 ACK 投递组件并校验配置。
     * Creates the ACK delivery component and validates its settings.
     *
     * @param adminClient DDC 管理端客户端; DDC administration client
     * @param properties  ACK 投递配置; ACK delivery settings
     * @throws IllegalArgumentException 依赖为空或配置无效时抛出; thrown when a dependency is null or settings are invalid
     */
    public DdcAckDelivery(DdcConfigClient adminClient,
                          DdcAckDeliveryProperties properties) {
        if (adminClient == null) {
            throw new IllegalArgumentException("adminClient must not be null");
        }
        if (properties == null) {
            throw new IllegalArgumentException("properties must not be null");
        }
        this.adminClient = adminClient;
        this.properties = properties;
        validateProperties();
    }

    /**
     * 提交一个 ACK，并在待处理映射中按变化、实例和租约去重。
     * Submits an ACK and deduplicates pending work by change, instance, and lease.
     *
     * @param request 待投递 ACK 请求; ACK request to deliver
     * @return 已接受或已存在相同待处理任务时为 {@code true}; {@code true} when accepted or already pending
     * @throws IllegalArgumentException 请求或幂等键字段无效时抛出; thrown when the request or idempotency-key fields are invalid
     */
    public boolean submit(DdcAckRequest request) {
        AckKey key = AckKey.from(request);
        PendingDelivery delivery;
        ScheduledThreadPoolExecutor current;
        synchronized (pendingMonitor) {
            current = executor;
            if (!running.get() || current == null || current.isShutdown()) {
                rejectedWhileStopped.incrementAndGet();
                return false;
            }
            PendingDelivery existing = pending.get(key);
            if (existing != null) {
                deduplicated.incrementAndGet();
                return true;
            }
            if (pending.size() >= properties.getQueueCapacity()) {
                saturated.incrementAndGet();
                LOGGER.warn(
                        "DDC ACK delivery queue saturated changeId={} instanceId={} "
                                + "leaseId={} pending={} capacity={}",
                        key.changeId(),
                        key.instanceId(),
                        key.leaseId(),
                        pending.size(),
                        properties.getQueueCapacity()
                );
                return false;
            }
            delivery = new PendingDelivery(
                    key,
                    request,
                    DdcTraceSupport.captureOrCreate()
            );
            pending.put(key, delivery);
            submitted.incrementAndGet();
        }
        if (!schedule(current, delivery, 0L)) {
            remove(delivery);
            rejectedWhileStopped.incrementAndGet();
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * 中文：启动用于 ACK 投递和延迟重试的守护线程执行器。
     * English: Starts the daemon executor used for ACK delivery and delayed retries.
     */
    @Override
    public synchronized void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        ScheduledThreadPoolExecutor created = new ScheduledThreadPoolExecutor(
                1,
                runnable -> {
                    Thread thread = new Thread(runnable, WORKER_NAME);
                    thread.setDaemon(true);
                    return thread;
                }
        );
        created.setRemoveOnCancelPolicy(true);
        created.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        created.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        executor = created;
        workerTerminated = false;
    }

    /**
     * {@inheritDoc}
     * 中文：停止接受任务、等待工作线程并清空仍待处理的 ACK。
     * English: Stops accepting tasks, waits for the worker, and clears remaining ACKs.
     */
    @Override
    public synchronized void stop() {
        if (!running.getAndSet(false) && executor == null) {
            return;
        }
        ScheduledThreadPoolExecutor current = executor;
        executor = null;
        if (current != null) {
            current.shutdown();
            awaitTermination(current, properties.getShutdownWait());
            if (!current.isTerminated()) {
                current.shutdownNow();
                awaitTermination(current, properties.getShutdownWait());
            }
            workerTerminated = current.isTerminated();
        } else {
            workerTerminated = true;
        }
        synchronized (pendingMonitor) {
            droppedOnShutdown.addAndGet(pending.size());
            pending.clear();
        }
    }

    /**
     * {@inheritDoc} 中文：同步停止后执行生命周期回调。 English: Runs the lifecycle callback after synchronous shutdown.
     */
    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    /**
     * {@inheritDoc} 中文：返回组件当前是否接受新任务。 English: Returns whether the component currently accepts new tasks.
     */
    @Override
    public boolean isRunning() {
        return running.get();
    }

    /**
     * {@inheritDoc} 中文：声明由 Spring 生命周期自动启动。 English: Declares automatic startup by the Spring lifecycle.
     */
    @Override
    public boolean isAutoStartup() {
        return true;
    }

    /**
     * {@inheritDoc} 中文：返回较晚启动、较早停止的生命周期阶段。 English: Returns a lifecycle phase that starts late and stops early.
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }

    /**
     * 关闭 ACK 投递组件。
     * Closes the ACK delivery component.
     */
    @Override
    public void close() {
        stop();
    }

    /**
     * 获取成功提交计数。
     * Returns the successful submission count.
     *
     * @return 成功提交数; successful submissions
     */
    public long submittedCount() {
        return submitted.get();
    }

    /**
     * 获取成功投递计数。
     * Returns the successful delivery count.
     *
     * @return 成功投递数; successful deliveries
     */
    public long deliveredCount() {
        return delivered.get();
    }

    /**
     * 获取已安排重试计数。
     * Returns the scheduled retry count.
     *
     * @return 重试数; retry count
     */
    public long retryCount() {
        return retried.get();
    }

    /**
     * 获取去重提交计数。
     * Returns the deduplicated submission count.
     *
     * @return 去重数; deduplicated count
     */
    public long deduplicatedCount() {
        return deduplicated.get();
    }

    /**
     * 获取队列饱和拒绝计数。
     * Returns the queue-saturation rejection count.
     *
     * @return 饱和拒绝数; saturation count
     */
    public long saturationCount() {
        return saturated.get();
    }

    /**
     * 获取重试耗尽计数。
     * Returns the retry-exhaustion count.
     *
     * @return 重试耗尽数; exhausted count
     */
    public long exhaustedCount() {
        return exhausted.get();
    }

    /**
     * 获取不可重试失败计数。
     * Returns the non-retryable failure count.
     *
     * @return 不可重试失败数; non-retryable failures
     */
    public long nonRetryableFailureCount() {
        return nonRetryableFailures.get();
    }

    /**
     * 获取停止期间拒绝计数。
     * Returns the rejection count while stopped.
     *
     * @return 停止期间拒绝数; stopped rejection count
     */
    public long rejectedWhileStoppedCount() {
        return rejectedWhileStopped.get();
    }

    /**
     * 获取停止时丢弃计数。
     * Returns the shutdown drop count.
     *
     * @return 停止时丢弃数; shutdown drop count
     */
    public long droppedOnShutdownCount() {
        return droppedOnShutdown.get();
    }

    /**
     * 获取当前待处理任务数。
     * Returns the current pending task count.
     *
     * @return 待处理任务数; pending task count
     */
    public int pendingCount() {
        synchronized (pendingMonitor) {
            return pending.size();
        }
    }

    /**
     * 判断当前或最近的工作执行器是否已完全终止。
     * Indicates whether the current or most recent worker executor has fully terminated.
     *
     * @return 工作线程已终止时为 {@code true}; {@code true} when the worker is terminated
     */
    public boolean isWorkerTerminated() {
        ScheduledThreadPoolExecutor current = executor;
        return current == null ? workerTerminated : current.isTerminated();
    }

    /**
     * 执行一次 ACK 投递并按结果完成或转入失败处理。
     * Performs one ACK delivery attempt and completes or delegates to failure handling.
     *
     * @param delivery 待投递任务; pending delivery
     */
    private void deliver(PendingDelivery delivery) {
        int attempt = delivery.attempts().incrementAndGet();
        try {
            adminClient.ack(delivery.request());
            delivered.incrementAndGet();
            remove(delivery);
        } catch (RuntimeException exception) {
            handleFailure(delivery, attempt, exception);
        }
    }

    /**
     * 根据异常类型、尝试次数和运行状态安排重试或终结任务。
     * Schedules a retry or terminates a task according to exception type, attempt count, and runtime state.
     *
     * @param delivery  投递任务; delivery task
     * @param attempt   已完成的尝试次数; completed attempt count
     * @param exception 投递异常; delivery exception
     */
    private void handleFailure(PendingDelivery delivery,
                               int attempt,
                               RuntimeException exception) {
        boolean retryable = isRetryable(exception);
        if (retryable && attempt < properties.getMaxAttempts() && running.get()) {
            long delayMs = retryDelayMs(attempt);
            ScheduledThreadPoolExecutor current = executor;
            if (current != null && schedule(current, delivery, delayMs)) {
                retried.incrementAndGet();
                LOGGER.warn(
                        "DDC ACK delivery retry scheduled changeId={} instanceId={} "
                                + "leaseId={} attempt={} delayMs={} cause={}",
                        delivery.key().changeId(),
                        delivery.key().instanceId(),
                        delivery.key().leaseId(),
                        attempt,
                        delayMs,
                        exception.getClass().getSimpleName()
                );
                return;
            }
        }
        remove(delivery);
        if (retryable && attempt >= properties.getMaxAttempts()) {
            exhausted.incrementAndGet();
            LOGGER.warn(
                    "DDC ACK delivery exhausted changeId={} instanceId={} leaseId={} "
                            + "attempts={} cause={}",
                    delivery.key().changeId(),
                    delivery.key().instanceId(),
                    delivery.key().leaseId(),
                    attempt,
                    exception.getClass().getSimpleName()
            );
        } else if (!retryable) {
            nonRetryableFailures.incrementAndGet();
            LOGGER.warn(
                    "DDC ACK delivery rejected without retry changeId={} instanceId={} "
                            + "leaseId={} attempt={} cause={}",
                    delivery.key().changeId(),
                    delivery.key().instanceId(),
                    delivery.key().leaseId(),
                    attempt,
                    exception.getClass().getSimpleName()
            );
        }
    }

    /**
     * 在指定执行器上按延迟安排一次携带原追踪上下文的投递。
     * Schedules a delivery with its captured trace context on the specified executor after a delay.
     *
     * @param current  目标执行器; target executor
     * @param delivery 投递任务; delivery task
     * @param delayMs  延迟毫秒数; delay in milliseconds
     * @return 成功安排时为 {@code true}; {@code true} when scheduling succeeds
     */
    private boolean schedule(ScheduledThreadPoolExecutor current,
                             PendingDelivery delivery,
                             long delayMs) {
        try {
            current.schedule(
                    DdcTraceSupport.wrapContext(
                            delivery.traceContext(),
                            "ack",
                            () -> deliver(delivery)
                    ),
                    delayMs,
                    TimeUnit.MILLISECONDS
            );
            return true;
        } catch (RejectedExecutionException exception) {
            return false;
        }
    }

    /**
     * 仅在键和值均匹配时从待处理映射移除任务。
     * Removes a task from the pending map only when both key and value match.
     *
     * @param delivery 待移除任务; delivery to remove
     */
    private void remove(PendingDelivery delivery) {
        synchronized (pendingMonitor) {
            pending.remove(delivery.key(), delivery);
        }
    }

    /**
     * 判断异常链是否包含网络访问异常或服务端 5xx 响应。
     * Determines whether the exception chain contains a network-access failure or server-side 5xx response.
     *
     * @param exception 投递异常; delivery exception
     * @return 异常可重试时为 {@code true}; {@code true} when the failure is retryable
     */
    private boolean isRetryable(RuntimeException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ResourceAccessException) {
                return true;
            }
            if (current instanceof RestClientResponseException responseException) {
                return responseException.getStatusCode().is5xxServerError();
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * 计算带有有界随机抖动的指数退避时长。
     * Computes an exponential-backoff delay with bounded random jitter.
     *
     * @param completedAttempts 已完成尝试次数; completed attempt count
     * @return 下一次尝试前的延迟毫秒数; delay in milliseconds before the next attempt
     */
    private long retryDelayMs(int completedAttempts) {
        long initial = properties.getInitialBackoff().toMillis();
        long maximum = properties.getMaxBackoff().toMillis();
        int shift = Math.min(completedAttempts - 1, 30);
        long multiplier = 1L << shift;
        long base = initial > Long.MAX_VALUE / multiplier
                ? maximum
                : Math.min(maximum, initial * multiplier);
        double jitter = properties.getJitter();
        if (jitter == 0.0) {
            return base;
        }
        double factor = 1.0 + ThreadLocalRandom.current()
                .nextDouble(-jitter, jitter);
        return Math.max(1L, Math.min(maximum, Math.round(base * factor)));
    }

    /**
     * 在指定时限内等待执行器终止，中断时恢复线程中断标记并强制停止。
     * Waits for executor termination within the timeout, restoring interruption and forcing shutdown when interrupted.
     *
     * @param current 待等待执行器; executor to await
     * @param timeout 最大等待时长; maximum wait duration
     */
    private void awaitTermination(ScheduledThreadPoolExecutor current,
                                  Duration timeout) {
        try {
            current.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            current.shutdownNow();
        }
    }

    /**
     * 校验队列容量、重试次数、退避、抖动和停止等待配置。
     * Validates queue capacity, attempt count, backoff, jitter, and shutdown-wait settings.
     *
     * @throws IllegalArgumentException 任一配置超出有效范围时抛出; thrown when any setting is outside its valid range
     */
    private void validateProperties() {
        if (properties.getQueueCapacity() <= 0) {
            throw new IllegalArgumentException("ACK queueCapacity must be positive");
        }
        if (properties.getMaxAttempts() <= 0) {
            throw new IllegalArgumentException("ACK maxAttempts must be positive");
        }
        requirePositive(properties.getInitialBackoff(), "initialBackoff");
        requirePositive(properties.getMaxBackoff(), "maxBackoff");
        requirePositive(properties.getShutdownWait(), "shutdownWait");
        if (properties.getInitialBackoff().compareTo(properties.getMaxBackoff()) > 0) {
            throw new IllegalArgumentException(
                    "ACK initialBackoff must not exceed maxBackoff"
            );
        }
        if (properties.getJitter() < 0.0 || properties.getJitter() >= 1.0) {
            throw new IllegalArgumentException("ACK jitter must be in [0, 1)");
        }
    }

    /**
     * 校验时长非空且为正值。
     * Validates that a duration is non-null and positive.
     *
     * @param value     待校验时长; duration to validate
     * @param fieldName 配置字段名; configuration field name
     * @throws IllegalArgumentException 时长无效时抛出; thrown when the duration is invalid
     */
    private void requirePositive(Duration value, String fieldName) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("ACK " + fieldName + " must be positive");
        }
    }

    /**
     * 唯一标识一条待投递 ACK 的复合键。
     * Composite key uniquely identifying a pending ACK delivery.
     *
     * @param changeId   发布变化标识; publication change identifier
     * @param instanceId 实例标识; instance identifier
     * @param leaseId    租约标识; lease identifier
     */
    private record AckKey(String changeId, String instanceId, String leaseId) {

        /**
         * 从 ACK 请求提取并校验复合键。
         * Extracts and validates a composite key from an ACK request.
         *
         * @param request ACK 请求; ACK request
         * @return 已校验复合键; validated composite key
         */
        private static AckKey from(DdcAckRequest request) {
            if (request == null) {
                throw new IllegalArgumentException("ACK request must not be null");
            }
            return new AckKey(
                    required(request.getChangeId(), "changeId"),
                    required(request.getInstanceId(), "instanceId"),
                    required(request.getLeaseId(), "leaseId")
            );
        }

        /**
         * 校验幂等键字段不为空白。
         * Validates that an idempotency-key field is nonblank.
         *
         * @param value     字段值; field value
         * @param fieldName 字段名; field name
         * @return 原字段值; original field value
         */
        private static String required(String value, String fieldName) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("ACK " + fieldName + " must not be blank");
            }
            return value;
        }
    }

    /**
     * 保存 ACK 请求、追踪上下文和累计尝试次数的待投递任务。
     * Pending delivery holding an ACK request, trace context, and accumulated attempt count.
     *
     * @param key          ACK 复合键; ACK composite key
     * @param request      ACK 请求; ACK request
     * @param traceContext 提交时捕获的追踪上下文; trace context captured at submission
     * @param attempts     已执行尝试次数; executed attempt count
     */
    private record PendingDelivery(
            AckKey key,
            DdcAckRequest request,
            TraceContext traceContext,
            AtomicInteger attempts
    ) {

        /**
         * 创建尝试次数从零开始的待投递任务。
         * Creates a pending delivery whose attempt count starts at zero.
         *
         * @param key          ACK 复合键; ACK composite key
         * @param request      ACK 请求; ACK request
         * @param traceContext 提交时捕获的追踪上下文; trace context captured at submission
         */
        private PendingDelivery(AckKey key,
                                DdcAckRequest request,
                                TraceContext traceContext) {
            this(key, request, traceContext, new AtomicInteger());
        }
    }
}
