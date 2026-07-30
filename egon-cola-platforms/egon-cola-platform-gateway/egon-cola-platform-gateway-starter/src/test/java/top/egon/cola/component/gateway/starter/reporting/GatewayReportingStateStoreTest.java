package top.egon.cola.component.gateway.starter.reporting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayReportingStateStoreTest {

    @TempDir
    private Path directory;

    @Test
    void persistsPendingReportAcrossProcessRestarts() {
        GatewayDefinitionReportFactory.BuiltReport report = report();
        Path stateFile = directory.resolve("report.state");
        GatewayReportingStateStore store = new GatewayReportingStateStore(
                stateFile
        );

        store.pending(
                report,
                report.identity().definitionFingerprint()
        );

        assertThat(new GatewayReportingStateStore(stateFile).load())
                .get()
                .satisfies(state -> {
                    assertThat(state.phase()).isEqualTo(
                            GatewayReportingStateStore.Phase.PENDING
                    );
                    assertThat(state.reportId()).isEqualTo(
                            report.report().reportId()
                    );
                });
    }

    @Test
    void isolatesCorruptedStateBeforeReportingAgain() throws Exception {
        Path stateFile = directory.resolve("report.state");
        Files.writeString(stateFile, "not=a-valid-state");
        GatewayReportingStateStore store = new GatewayReportingStateStore(
                stateFile,
                Clock.fixed(Instant.ofEpochMilli(42), ZoneOffset.UTC)
        );

        assertThat(store.load()).isEmpty();
        assertThat(stateFile).doesNotExist();
        assertThat(directory.resolve("report.state.corrupt-42")).exists();
    }

    private GatewayDefinitionReportFactory.BuiltReport report() {
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
        return new GatewayDefinitionReportFactory(
                properties,
                Clock.fixed(
                        Instant.parse("2026-07-25T00:00:00Z"),
                        ZoneOffset.UTC
                )
        ).build(List.of());
    }
}
