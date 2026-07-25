package top.egon.cola.component.gateway.starter.reporting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReportResult;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GatewayReportingCoordinatorTest {

    @TempDir
    private Path directory;

    private GatewayReportingCoordinator coordinator;

    @AfterEach
    void stopCoordinator() {
        if (coordinator != null) {
            coordinator.stop();
        }
    }

    @Test
    void reconcilesAnUncertainPostThroughTheReceiptEndpoint()
            throws Exception {
        GatewayReportingProperties properties = properties();
        GatewayDefinitionReportFactory.BuiltReport report =
                report(properties);
        GatewayInterfaceDefinitionReportResult receipt = receipt(
                report.report().reportId(),
                report.report().definitionSetId()
        );
        GatewayReportHttpClient client =
                mock(GatewayReportHttpClient.class);
        when(client.submit(report))
                .thenThrow(new GatewayReportHttpClient
                        .GatewayReportTransportException(
                        "connection reset",
                        true,
                        null
                ));
        CountDownLatch reconciled = new CountDownLatch(1);
        when(client.find(report.report().reportId()))
                .thenAnswer(invocation -> {
                    reconciled.countDown();
                    return Optional.of(receipt);
                });
        GatewayReportingState reportingState =
                new GatewayReportingState();
        coordinator = new GatewayReportingCoordinator(
                properties,
                report,
                client,
                reportingState,
                new GatewayReportingStateStore(
                        directory.resolve("report.state")
                )
        );

        coordinator.start();
        coordinator.reconcile(report);

        assertThat(reconciled.await(2, TimeUnit.SECONDS)).isTrue();
        awaitSuccess(reportingState);
        verify(client).submit(report);
        assertThat(reportingState.snapshot().result()).isEqualTo(receipt);
    }

    @Test
    void restoresAPendingReportBeforeSubmittingANewProcessReport()
            throws Exception {
        GatewayReportingProperties properties = properties();
        GatewayDefinitionReportFactory.BuiltReport oldReport =
                report(properties);
        GatewayDefinitionReportFactory.BuiltReport currentReport =
                report(properties);
        GatewayReportingStateStore store = new GatewayReportingStateStore(
                directory.resolve("report.state")
        );
        store.pending(
                oldReport,
                oldReport.identity().definitionFingerprint()
        );
        GatewayInterfaceDefinitionReportResult receipt = receipt(
                oldReport.report().reportId(),
                oldReport.report().definitionSetId()
        );
        GatewayReportHttpClient client =
                mock(GatewayReportHttpClient.class);
        CountDownLatch reconciled = new CountDownLatch(1);
        when(client.find(oldReport.report().reportId()))
                .thenAnswer(invocation -> {
                    reconciled.countDown();
                    return Optional.of(receipt);
                });
        GatewayReportingState reportingState =
                new GatewayReportingState();
        coordinator = new GatewayReportingCoordinator(
                properties,
                currentReport,
                client,
                reportingState,
                store
        );

        coordinator.start();
        coordinator.reconcile(currentReport);

        assertThat(reconciled.await(1, TimeUnit.SECONDS)).isTrue();
        awaitSuccess(reportingState);
        verify(client, never()).submit(any());
        assertThat(reportingState.snapshot().result()).isEqualTo(receipt);
    }

    private void awaitSuccess(GatewayReportingState state)
            throws InterruptedException {
        long deadline = System.nanoTime()
                + TimeUnit.SECONDS.toNanos(1);
        while (!"SUCCESS".equals(state.snapshot().status())
                && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertThat(state.snapshot().status()).isEqualTo("SUCCESS");
    }

    private GatewayInterfaceDefinitionReportResult receipt(
            String reportId,
            String definitionSetId) {
        return new GatewayInterfaceDefinitionReportResult(
                reportId,
                definitionSetId,
                GatewayInterfaceDefinitionReportResult.Status.ACCEPTED,
                "app-1",
                new GatewayInterfaceDefinitionReportResult.Counts(
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0
                ),
                List.of(),
                List.of(),
                Instant.parse("2026-07-25T00:00:01Z")
        );
    }

    private GatewayDefinitionReportFactory.BuiltReport report(
            GatewayReportingProperties properties) {
        return new GatewayDefinitionReportFactory(
                properties,
                Clock.fixed(
                        Instant.parse("2026-07-25T00:00:00Z"),
                        ZoneOffset.UTC
                )
        ).build(List.of());
    }

    private GatewayReportingProperties properties() {
        GatewayReportingProperties properties =
                new GatewayReportingProperties();
        properties.setEnabled(true);
        properties.setAdminBaseUrl("http://admin.local");
        properties.setApplicationCode("inventory");
        properties.setApplicationName("Inventory");
        properties.setEnv("test");
        properties.setNamespace("default");
        properties.setArtifactVersion("1.0.0");
        properties.setBuildId("build-1");
        properties.setAccessKey("access-key");
        properties.setSecretKey("secret-key");
        properties.setMaxAttempts(1);
        properties.setReconcileInterval(Duration.ofMillis(10));
        return properties;
    }
}
