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

public final class GatewayReportingCoordinator
        implements ApplicationListener<ApplicationReadyEvent>,
        SmartLifecycle {

    private final GatewayReportingProperties properties;

    private final GatewayReportHttpClient client;

    private final GatewayReportingState state;

    private final GatewayReportingStateStore stateStore;

    private final AtomicReference<GatewayDefinitionReportFactory.BuiltReport>
            desiredReport;

    private final ScheduledExecutorService worker =
            Executors.newSingleThreadScheduledExecutor(
                    Thread.ofPlatform()
                            .name("gateway-definition-reporter")
                            .daemon(true)
                            .factory()
            );

    private final AtomicBoolean running = new AtomicBoolean();

    private final AtomicBoolean workScheduled = new AtomicBoolean();

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
     */
    public void reconcile(
            GatewayDefinitionReportFactory.BuiltReport report) {
        desiredReport.set(report);
        if (running.get() && workScheduled.compareAndSet(false, true)) {
            worker.execute(() -> attempt(report, 1));
        }
    }

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

    private boolean sameDefinition(
            GatewayDefinitionReportFactory.BuiltReport left,
            GatewayDefinitionReportFactory.BuiltReport right) {
        return left.identity().definitionFingerprint().equals(
                right.identity().definitionFingerprint()
        ) && left.report().definitionSetId().equals(
                right.report().definitionSetId()
        );
    }

    @Override
    public void start() {
        running.set(true);
    }

    @Override
    public void stop() {
        running.set(false);
        worker.shutdownNow();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }
}
