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
 */
public final class GatewayReportingCoordinator
        implements ApplicationListener<ApplicationReadyEvent>,
        SmartLifecycle {

    /** Reporting and retry configuration. */
    private final GatewayReportingProperties properties;

    /** Client used to submit reports and query acknowledgement receipts. */
    private final GatewayReportHttpClient client;

    /** In-memory reporting lifecycle state. */
    private final GatewayReportingState state;

    /** Persistent pending and acknowledged report state. */
    private final GatewayReportingStateStore stateStore;

    /** Most recent definition report that must be acknowledged. */
    private final AtomicReference<GatewayDefinitionReportFactory.BuiltReport>
            desiredReport;

    /** Single-threaded worker that serializes reporting and retry work. */
    private final ScheduledExecutorService worker =
            Executors.newSingleThreadScheduledExecutor(
                    Thread.ofPlatform()
                            .name("gateway-definition-reporter")
                            .daemon(true)
                            .factory()
            );

    /** Whether the Spring lifecycle component is currently running. */
    private final AtomicBoolean running = new AtomicBoolean();

    /** Whether reporting work is queued or executing on the worker. */
    private final AtomicBoolean workScheduled = new AtomicBoolean();

    /**
     * Creates a reporting coordinator for an initially discovered report.
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

    /** Starts accepting and scheduling reporting work. */
    @Override
    public void start() {
        running.set(true);
    }

    /** Stops reporting and cancels queued worker tasks. */
    @Override
    public void stop() {
        running.set(false);
        worker.shutdownNow();
    }

    /**
     * Returns whether the reporting lifecycle component is active.
     *
     * @return {@code true} while reporting is active
     */
    @Override
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Runs reporting late in startup and stops it early during shutdown.
     *
     * @return lifecycle phase
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }
}
