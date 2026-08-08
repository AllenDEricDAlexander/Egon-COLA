package top.egon.cola.component.gateway.starter.reporting;

import org.springframework.context.SmartLifecycle;
import top.egon.cola.component.gateway.contract.reporting
        .GatewayInterfaceDefinitionReportResult;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Submits the discovered Gateway definition report during application startup.
 *
 * <p>Every startup submits the current report. The coordinator makes at most
 * three immediate attempts and fails the application startup when none of them
 * receives a valid acknowledgement. No report state is persisted locally.
 *
 * <p>中文：在应用启动阶段提交已发现的网关接口定义报告。每次启动都会上报当前
 * 报告，最多立即尝试三次；三次均未收到有效确认时，直接使应用启动失败。本地
 * 不持久化上报状态。
 */
public final class GatewayReportingCoordinator implements SmartLifecycle {

    /** Fixed number of startup report attempts. 启动上报的固定尝试次数。 */
    static final int MAX_STARTUP_ATTEMPTS = 3;

    /** Report built from the current application definitions. 当前应用定义报告。 */
    private final GatewayDefinitionReportFactory.BuiltReport report;

    /** HTTP client used to submit the report to Gateway Admin. 上报 HTTP 客户端。 */
    private final GatewayReportHttpClient client;

    /** In-memory reporting lifecycle state. 内存中的上报生命周期状态。 */
    private final GatewayReportingState state;

    /** Whether the lifecycle component has started successfully. 是否已成功启动。 */
    private final AtomicBoolean running = new AtomicBoolean();

    /**
     * Creates a startup reporting coordinator.
     * 创建启动阶段的上报协调器。
     *
     * @param report report to submit，要提交的报告
     * @param client reporting HTTP client，上报 HTTP 客户端
     * @param state in-memory reporting state，内存上报状态
     */
    public GatewayReportingCoordinator(
            GatewayDefinitionReportFactory.BuiltReport report,
            GatewayReportHttpClient client,
            GatewayReportingState state) {
        this.report = report;
        this.client = client;
        this.state = state;
    }

    /**
     * Submits the report synchronously before startup is considered complete.
     * 在启动完成前同步提交报告。
     *
     * @throws IllegalStateException when all startup attempts fail
     */
    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            submitWithRetries();
        } catch (RuntimeException failure) {
            running.set(false);
            throw failure;
        }
    }

    /**
     * Performs the fixed three-attempt startup submission.
     * 执行固定三次的启动上报。
     *
     * @throws IllegalStateException when no attempt receives a valid receipt
     */
    private void submitWithRetries() {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_STARTUP_ATTEMPTS; attempt++) {
            state.attempting(attempt);
            try {
                GatewayInterfaceDefinitionReportResult result =
                        client.submit(report);
                validateReceipt(
                        result,
                        report.report().reportId(),
                        report.report().definitionSetId()
                );
                state.success(result);
                return;
            } catch (RuntimeException failure) {
                lastFailure = failure;
                state.failure(failure.getMessage());
            }
        }
        throw new IllegalStateException(
                "gateway definition report failed after "
                        + MAX_STARTUP_ATTEMPTS
                        + " attempts",
                lastFailure
        );
    }

    /**
     * Verifies that Gateway Admin accepted the exact report being submitted.
     * 校验 Gateway Admin 是否确认了当前提交的准确报告。
     *
     * @param result acknowledgement returned by Admin，Admin 返回的确认结果
     * @param expectedReportId expected report identifier，期望的报告标识
     * @param expectedDefinitionSetId expected definition set identifier，期望的定义集合标识
     * @throws GatewayReportHttpClient.GatewayReportTransportException when the
     *         acknowledgement is absent, rejected, or identifies another report
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
                    null
            );
        }
    }

    /**
     * Stops the lifecycle component and clears its running state.
     * 停止生命周期组件并清除运行状态。
     */
    @Override
    public void stop() {
        running.set(false);
    }

    /**
     * Returns whether the startup report has completed successfully.
     * 返回启动报告是否已经成功完成。
     *
     * @return {@code true} after a successful report
     */
    @Override
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Starts reporting before the embedded web server lifecycle.
     * 在内嵌 Web 服务生命周期之前执行上报。
     *
     * @return lifecycle phase
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }
}
