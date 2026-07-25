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

public final class GatewayReportingCoordinator
        implements ApplicationListener<ApplicationReadyEvent>,
        SmartLifecycle {

    private final GatewayReportingProperties properties;

    private final GatewayDefinitionReportFactory.BuiltReport report;

    private final GatewayReportHttpClient client;

    private final GatewayReportingState state;

    private final ScheduledExecutorService worker =
            Executors.newSingleThreadScheduledExecutor(
                    Thread.ofPlatform()
                            .name("gateway-definition-reporter")
                            .daemon(true)
                            .factory()
            );

    private final AtomicBoolean running = new AtomicBoolean();

    public GatewayReportingCoordinator(
            GatewayReportingProperties properties,
            GatewayDefinitionReportFactory.BuiltReport report,
            GatewayReportHttpClient client,
            GatewayReportingState state) {
        this.properties = properties;
        this.report = report;
        this.client = client;
        this.state = state;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (properties.isFailFast()) {
            report(1);
        } else {
            worker.execute(() -> attempt(1));
        }
    }

    private void attempt(int attempt) {
        if (!running.get()) {
            return;
        }
        try {
            report(attempt);
        } catch (GatewayReportHttpClient.GatewayReportTransportException
                failure) {
            state.failure(failure.getMessage());
            if (failure.retryable()
                    && attempt < properties.getMaxAttempts()
                    && running.get()) {
                worker.schedule(
                        () -> attempt(attempt + 1),
                        backoff(attempt).toMillis(),
                        TimeUnit.MILLISECONDS
                );
            }
        } catch (RuntimeException failure) {
            state.failure(failure.getMessage());
        }
    }

    private void report(int attempt) {
        state.attempting(attempt);
        GatewayInterfaceDefinitionReportResult result =
                client.submit(report);
        if (result == null
                || result.status()
                == GatewayInterfaceDefinitionReportResult.Status.REJECTED) {
            throw new GatewayReportHttpClient.GatewayReportTransportException(
                    "gateway report was rejected",
                    false,
                    null
            );
        }
        state.success(result);
    }

    private Duration backoff(int attempt) {
        long seconds = Math.min(30, 1L << Math.min(attempt - 1, 5));
        long jitter = Math.floorMod(
                report.report().reportId().hashCode() + attempt,
                500
        );
        return Duration.ofSeconds(seconds).plusMillis(jitter);
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
