package top.egon.cola.component.gateway.test.process;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayProcessHarnessTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void builderProducesImmutableRedactedDiagnosticSpec() {
        GatewayProcessSpec spec = GatewayProcessSpec.builder(
                        "admin",
                        "example.AdminApplication"
                )
                .argument("server.port", 18080)
                .argument("gateway.admin.secret-key", "do-not-log")
                .environment("DATABASE_PASSWORD", "do-not-log-either")
                .startupTimeout(Duration.ofSeconds(30))
                .build();

        assertThat(spec.arguments()).contains(
                "--gateway.admin.secret-key=do-not-log"
        );
        assertThat(spec.redactedArguments())
                .contains("--gateway.admin.secret-key=******")
                .doesNotContain("--gateway.admin.secret-key=do-not-log");
        assertThat(spec.redactedEnvironment())
                .containsEntry("DATABASE_PASSWORD", "******");
        assertThat(spec.startupTimeout()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void rejectsUnsafeProcessArtifactName() {
        assertThatThrownBy(() -> GatewayProcessSpec.builder(
                        "../outside",
                        "example.Application"
                )
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void prioritizesTheMainApplicationClasspathEntry() {
        Path gatewayEngine = temporaryDirectory.resolve("gateway-engine.jar");
        Path ddcAdmin = temporaryDirectory.resolve("ddc-admin.jar");
        String classPath = String.join(
                File.pathSeparator,
                gatewayEngine.toString(),
                ddcAdmin.toString()
        );

        assertThat(GatewayProcessHarness.prioritizeClassPath(
                classPath,
                ddcAdmin
        )).startsWith(ddcAdmin + File.pathSeparator);
    }

    @Test
    void resolvesAnAttachedExecutableArchive() throws Exception {
        Path thinArchive = temporaryDirectory.resolve("gateway-admin.jar");
        Path executableArchive = temporaryDirectory.resolve(
                "gateway-admin-exec.jar"
        );
        Files.createFile(thinArchive);
        Files.createFile(executableArchive);

        assertThat(GatewayProcessHarness.executableArchive(thinArchive))
                .contains(executableArchive);
    }

    @Test
    void isolatesArtifactsAndRestartsAStoppedProcess() throws Exception {
        try (GatewayProcessHarness harness = new GatewayProcessHarness(
                temporaryDirectory.resolve("restart"),
                Duration.ofSeconds(2),
                Duration.ofSeconds(2))) {
            GatewayProcessHarness.ChildProcess engineOne = harness.start(
                    probeSpec("gateway-engine-1", false, null)
            );
            GatewayProcessHarness.ChildProcess engineTwo = harness.start(
                    probeSpec("gateway-engine-2", false, null)
            );
            awaitReady(harness, engineOne);
            awaitReady(harness, engineTwo);

            assertThat(engineOne.logFile())
                    .isNotEqualTo(engineTwo.logFile());
            assertThat(engineOne.manifestFile())
                    .isNotEqualTo(engineTwo.manifestFile());
            assertThat(engineOne.lkgDirectory())
                    .isNotEqualTo(engineTwo.lkgDirectory());

            long firstPid = engineOne.process().pid();
            Path firstLog = engineOne.logFile();
            harness.kill(engineOne);
            GatewayProcessHarness.ChildProcess restarted =
                    harness.restart(engineOne);
            awaitReady(harness, restarted);

            assertThat(restarted.process().pid()).isNotEqualTo(firstPid);
            assertThat(restarted.logFile()).isNotEqualTo(firstLog);
            assertThat(restarted.lkgDirectory())
                    .isEqualTo(engineOne.lkgDirectory());
            assertThat(firstLog).exists();
            assertThat(restarted.logFile()).exists();
        }
    }

    @Test
    void closesInReverseOrderAndPreservesForcedStopLog() throws Exception {
        Path shutdownLog = temporaryDirectory.resolve("shutdown-order.log");
        GatewayProcessHarness harness = new GatewayProcessHarness(
                temporaryDirectory.resolve("close"),
                Duration.ofSeconds(2),
                Duration.ofSeconds(2)
        );
        GatewayProcessHarness.ChildProcess first = harness.start(
                probeSpec("first", false, shutdownLog)
        );
        GatewayProcessHarness.ChildProcess stubborn = harness.start(
                probeSpec("stubborn", true, shutdownLog)
        );
        awaitReady(harness, first);
        awaitReady(harness, stubborn);

        harness.close();

        assertThat(first.process().isAlive()).isFalse();
        assertThat(stubborn.process().isAlive()).isFalse();
        assertThat(Files.readAllLines(shutdownLog))
                .containsExactly("stubborn", "first");
        assertThat(harness.output(stubborn)).contains("READY stubborn");
        assertThat(stubborn.logFile()).exists();
    }

    private GatewayProcessSpec probeSpec(
            String name,
            boolean blockShutdown,
            Path shutdownLog) {
        GatewayProcessSpec.Builder builder = GatewayProcessSpec.builder(
                        name,
                        GatewayProcessProbe.class.getName()
                )
                .rawArgument("--name=" + name)
                .rawArgument("--block-shutdown=" + blockShutdown)
                .startupTimeout(Duration.ofSeconds(5));
        if (shutdownLog != null) {
            builder.rawArgument("--shutdown-log=" + shutdownLog);
        }
        return builder.build();
    }

    private void awaitReady(
            GatewayProcessHarness harness,
            GatewayProcessHarness.ChildProcess process) {
        harness.awaitCondition(
                () -> harness.output(process).contains(
                        "READY " + process.name()
                ),
                Duration.ofSeconds(5),
                process.name() + " probe readiness"
        );
    }
}
