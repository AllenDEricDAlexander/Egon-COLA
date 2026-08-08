package top.egon.cola.component.gateway.starter.reporting;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.SmartLifecycle;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReportResult;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Coordinates startup reporting, receipt reconciliation, and retry scheduling
 * for the currently desired Gateway definition report.
 *
 * <p>中文：协调启动上报、回执对账以及当前目标接口定义报告的重试
 * 调度。
 */
public final class GatewayReportingCoordinator
        implements ApplicationListener<ApplicationReadyEvent>,
        SmartLifecycle {

    /** Reporting and retry configuration. 上报及重试配置。 */
    private final GatewayReportingProperties properties;

    /**
     * Client used to submit reports and query acknowledgement receipts.
     * 提交报告并查询确认回执的客户端。
     */
    private final GatewayReportHttpClient client;

    /** In-memory reporting lifecycle state. 内存中的上报生命周期状态。 */
    private final GatewayReportingState state;

    /**
     * Persistent pending and acknowledged report state.
     * 待处理及已确认报告的持久化状态。
     */
    private final GatewayReportingStateStore stateStore;

    /**
     * Most recent definition report that must be acknowledged.
     * 最近需要确认的接口定义报告。
     */
    private final AtomicReference<GatewayDefinitionReportFactory.BuiltReport>
            desiredReport;

    /**
     * Single-threaded worker that serializes reporting and retry work.
     * 串行执行上报与重试任务的单线程工作器。
     */
    private final ScheduledExecutorService worker =
            Executors.newSingleThreadScheduledExecutor(
                    Thread.ofPlatform()
                            .name("gateway-definition-reporter")
                            .daemon(true)
                            .factory()
            );

    /**
     * Whether the Spring lifecycle component is currently running.
     * Spring 生命周期组件是否正在运行。
     */
    private final AtomicBoolean running = new AtomicBoolean();

    /**
     * Whether reporting work is queued or executing on the worker.
     * 上报任务是否已排队或正在工作器中执行。
     */
    private final AtomicBoolean workScheduled = new AtomicBoolean();

    /**
     * Creates a reporting coordinator for an initially discovered report.
     * 中文：为首次发现的接口定义报告创建上报协调器。
     *
     * @param properties reporting and retry configuration
     * @param report initially desired report
     * @param client reporting HTTP client
     * @param state in-memory reporting state
     * @param stateStore persistent reporting state store
     */
    public GatewayReportingCoordinator(
            GatewayReportingProperties properties,
            GatewayDefinitionReportFactory.BuiltReport report,
            GatewayReportHttpClient client,
            GatewayReportingState state,
            GatewayReportingStateStore stateStore) {
        this.properties = properties;
        this.client = client;
        this.state = state;
        this.stateStore = stateStore;
        this.desiredReport = new AtomicReference<>(report);
    }

    /**
     * Starts initial reconciliation when the Spring application is ready.
     * 中文：Spring 应用就绪后开始首次报告协调。
     *
     * @param event application-ready event
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (properties.isFailFast()) {
            reconcileOnce(desiredReport.get(), 1);
        } else {
            reconcile(desiredReport.get());
        }
    }

    /**
     * Signals that a newly discovered definition set must be acknowledged.
     * 中文：通知协调器需要确认新发现的接口定义集合。
     *
     * @param report newly desired definition report
     */
    public void reconcile(
            GatewayDefinitionReportFactory.BuiltReport report) {
        desiredReport.set(report);
        if (running.get() && workScheduled.compareAndSet(false, true)) {
            worker.execute(() -> attempt(report, 1));
        }
    }

    /**
     * Attempts reconciliation for the latest desired report and schedules the
     * appropriate short or long retry after a failure.
     * 中文：尝试协调最新目标报告，并在失败后安排短重试或长期重试。
     *
     * @param scheduledReport report associated with the queued work
     * @param attempt queued attempt number
     */
    private void attempt(
            GatewayDefinitionReportFactory.BuiltReport scheduledReport,
            int attempt) {
        if (!running.get()) {
            workScheduled.set(false);
            return;
        }
        GatewayDefinitionReportFactory.BuiltReport current =
                desiredReport.get();
        int currentAttempt = sameDefinition(current, scheduledReport)
                ? attempt
                : 1;
        try {
            reconcileOnce(current, currentAttempt);
            workScheduled.set(false);
            GatewayDefinitionReportFactory.BuiltReport latest =
                    desiredReport.get();
            if (!sameDefinition(latest, current)) {
                reconcile(latest);
            }
        } catch (GatewayReportHttpClient.GatewayReportTransportException
                failure) {
            state.failure(failure.getMessage());
            if (!failure.retryable()) {
                workScheduled.set(false);
                return;
            }
            scheduleRetry(
                    current,
                    currentAttempt < properties.getMaxAttempts()
                            ? currentAttempt + 1
                            : 1,
                    currentAttempt < properties.getMaxAttempts()
                            ? backoff(current, currentAttempt)
                            : properties.getReconcileInterval()
            );
        } catch (RuntimeException failure) {
            state.failure(failure.getMessage());
            scheduleRetry(
                    current,
                    1,
                    properties.getReconcileInterval()
            );
        }
    }

    /**
     * Reconciles one report by reusing a valid stored receipt or submitting a
     * new report and persisting its acknowledgement.
     * 中文：优先复用有效的持久化回执，否则重新提交报告并保存确认
     * 结果。
     *
     * @param report report to reconcile
     * @param attempt current attempt number
     * @throws RuntimeException if persistence, transport, or receipt validation
     *                          fails
     */
    private void reconcileOnce(
            GatewayDefinitionReportFactory.BuiltReport report,
            int attempt) {
        state.attempting(attempt);
        String payloadHash = report.identity().definitionFingerprint();
        GatewayReportingStateStore.StoredState stored = stateStore.load()
                .filter(value -> value.matches(report, payloadHash))
                .orElse(null);
        if (stored != null) {
            GatewayInterfaceDefinitionReportResult receipt =
                    client.find(stored.reportId()).orElse(null);
            if (receipt != null) {
                validateReceipt(
                        receipt,
                        stored.reportId(),
                        report.report().definitionSetId()
                );
                stateStore.acknowledged(payloadHash, receipt);
                state.success(receipt);
                return;
            }
        }
        if (stored == null
                || !stored.reportId().equals(report.report().reportId())) {
            stateStore.pending(report, payloadHash);
        }
        GatewayInterfaceDefinitionReportResult result =
                client.submit(report);
        validateReceipt(
                result,
                report.report().reportId(),
                report.report().definitionSetId()
        );
        stateStore.acknowledged(payloadHash, result);
        state.success(result);
    }

    /**
     * Verifies that a receipt accepts the expected report and definition set.
     * 中文：校验回执是否确认了预期的报告及接口定义集合。
     *
     * @param result receipt returned by Gateway Admin
     * @param expectedReportId expected report identifier
     * @param expectedDefinitionSetId expected definition set identifier
     * @throws GatewayReportHttpClient.GatewayReportTransportException if the
     *         receipt is absent, rejected, or identifies another report
     */
    private void validateReceipt(
            GatewayInterfaceDefinitionReportResult result,
            String expectedReportId,
            String expectedDefinitionSetId) {
        if (result == null
                || result.status()
                == GatewayInterfaceDefinitionReportResult.Status.REJECTED
                || !expectedReportId.equals(result.reportId())
                || !expectedDefinitionSetId.equals(
                result.definitionSetId()
        )) {
            throw new GatewayReportHttpClient.GatewayReportTransportException(
                    "gateway report acknowledgement is invalid",
                    false,
                    null
            );
        }
    }

    /**
     * Schedules a future reconciliation attempt while the component is active.
     * 中文：组件运行期间安排一次未来的报告协调尝试。
     *
     * @param report report associated with the retry
     * @param attempt next attempt number
     * @param delay delay before retrying
     */
    private void scheduleRetry(
            GatewayDefinitionReportFactory.BuiltReport report,
            int attempt,
            Duration delay) {
        if (!running.get()) {
            workScheduled.set(false);
            return;
        }
        worker.schedule(
                () -> attempt(report, attempt),
                delay.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Computes capped exponential retry delay with stable report-based jitter.
     * 中文：计算带上限的指数退避时延，并使用报告标识生成稳定抖动。
     *
     * @param report report being retried
     * @param attempt failed attempt number
     * @return retry delay
     */
    private Duration backoff(
            GatewayDefinitionReportFactory.BuiltReport report,
            int attempt) {
        long seconds = Math.min(30, 1L << Math.min(attempt - 1, 5));
        long jitter = Math.floorMod(
                report.report().reportId().hashCode() + attempt,
                500
        );
        return Duration.ofSeconds(seconds).plusMillis(jitter);
    }

    /**
     * Tests whether two built reports describe the same definition revision.
     * 中文：判断两个已构建报告是否描述同一个接口定义版本。
     *
     * @param left first report
     * @param right second report
     * @return {@code true} when both identities describe the same definition
     */
    private boolean sameDefinition(
            GatewayDefinitionReportFactory.BuiltReport left,
            GatewayDefinitionReportFactory.BuiltReport right) {
        return left.identity().definitionFingerprint().equals(
                right.identity().definitionFingerprint()
        ) && left.report().definitionSetId().equals(
                right.report().definitionSetId()
        );
    }

    /** Starts accepting and scheduling reporting work. 开始接受并调度上报任务。 */
    @Override
    public void start() {
        running.set(true);
    }

    /**
     * Stops reporting and cancels queued worker tasks.
     * 停止上报并取消工作器中的排队任务。
     */
    @Override
    public void stop() {
        running.set(false);
        worker.shutdownNow();
    }

    /**
     * Returns whether the reporting lifecycle component is active.
     * 中文：返回上报生命周期组件是否处于活动状态。
     *
     * @return {@code true} while reporting is active
     */
    @Override
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Runs reporting late in startup and stops it early during shutdown.
     * 中文：使上报在启动后段运行，并在关闭早期停止。
     *
     * @return lifecycle phase
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }
}
