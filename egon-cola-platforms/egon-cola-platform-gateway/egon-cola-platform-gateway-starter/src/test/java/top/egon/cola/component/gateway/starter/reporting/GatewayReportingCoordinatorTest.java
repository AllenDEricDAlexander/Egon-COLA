package top.egon.cola.component.gateway.starter.reporting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.reporting
        .GatewayInterfaceDefinitionReportResult;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GatewayReportingCoordinatorTest {

    private GatewayReportingCoordinator coordinator;

    @AfterEach
    void stopCoordinator() {
        if (coordinator != null) {
            coordinator.stop();
        }
    }

    @Test
    void submitsEveryStartupReportSynchronously() {
        GatewayReportingProperties properties = properties();
        GatewayDefinitionReportFactory.BuiltReport report = report(properties);
        GatewayInterfaceDefinitionReportResult receipt = receipt(report);
        GatewayReportHttpClient client = mock(GatewayReportHttpClient.class);
        when(client.submit(report)).thenReturn(receipt);
        GatewayReportingState state = new GatewayReportingState();
        coordinator = new GatewayReportingCoordinator(
                report,
                client,
                state
        );

        coordinator.start();

        verify(client).submit(report);
        assertThat(coordinator.isRunning()).isTrue();
        assertThat(state.snapshot().status()).isEqualTo("SUCCESS");
        assertThat(state.snapshot().result()).isEqualTo(receipt);
    }

    @Test
    void retriesExactlyThreeTimesThenFailsStartup() {
        GatewayReportingProperties properties = properties();
        GatewayDefinitionReportFactory.BuiltReport report = report(properties);
        GatewayReportHttpClient client = mock(GatewayReportHttpClient.class);
        when(client.submit(report)).thenThrow(new RuntimeException("offline"));
        GatewayReportingState state = new GatewayReportingState();
        coordinator = new GatewayReportingCoordinator(
                report,
                client,
                state
        );

        assertThatThrownBy(coordinator::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("3 attempts");

        verify(client, times(3)).submit(report);
        assertThat(coordinator.isRunning()).isFalse();
        assertThat(state.snapshot().status()).isEqualTo("FAILED");
        assertThat(state.snapshot().attempt()).isEqualTo(3);
    }

    private GatewayInterfaceDefinitionReportResult receipt(
            GatewayDefinitionReportFactory.BuiltReport report) {
        return new GatewayInterfaceDefinitionReportResult(
                report.report().reportId(),
                report.report().definitionSetId(),
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
        properties.setBizCode("test-biz");
        properties.setApplicationName("Inventory");
        properties.setEnv("test");
        properties.setNamespace("default");
        properties.setArtifactVersion("1.0.0");
        properties.setBuildId("build-1");
        properties.setAccessKey("access-key");
        properties.setSecretKey("secret-key");
        return properties;
    }
}
