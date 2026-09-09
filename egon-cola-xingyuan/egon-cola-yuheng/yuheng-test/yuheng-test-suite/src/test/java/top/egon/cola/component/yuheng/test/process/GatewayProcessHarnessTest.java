package top.egon.cola.component.yuheng.test.process;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import top.egon.cola.component.yuheng.contract.runtime.GatewayEngineRoleEnum;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayProcessHarnessTest {

    @Test
    void engineSpecsCarryExplicitRoleArtifactAndSeparateEndpoints() {
        assertThat(GatewayProcessSpec.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .contains("engineRole", "artifactId", "dataPlaneBaseUri", "managementBaseUri");
    }

    @Test
    void mapsEachExplicitRoleToItsOwnMainClassAndArtifact() {
        var api = engine("api", GatewayEngineRoleEnum.API_RPC, 18081, 18083);
        var mcp = engine("mcp", GatewayEngineRoleEnum.MCP, 18084, 18085);
        assertThat(api.artifactId()).isEqualTo("yuheng-biz-gateway");
        assertThat(mcp.artifactId()).isEqualTo("yuheng-mcp-gateway");
        assertThat(api.mainClass()).isEqualTo("top.egon.cola.component.yuheng.engine.GatewayEngineApplication");
        assertThat(mcp.mainClass()).isEqualTo("top.egon.cola.component.yuheng.mcp.engine.McpGatewayEngineApplication");
        assertThat(api.dataPlaneBaseUri()).isNotEqualTo(mcp.dataPlaneBaseUri());
        assertThat(api.managementBaseUri()).isNotEqualTo(mcp.managementBaseUri());
        assertThatThrownBy(() -> GatewayProcessSpec.builder("mixed", api.mainClass()).build())
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("explicit role");
        assertThatThrownBy(() -> new GatewayProcessSpec("bad", api.mainClass(), List.of(), Map.of(),
                Duration.ofSeconds(30), GatewayEngineRoleEnum.MCP, api.artifactId(),
                mcp.dataPlaneBaseUri(), mcp.managementBaseUri()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("must agree");
    }

    @Test
    void rejectsDuplicatePortsUnsafeEndpointsAndRunningIdentities() {
        assertThatThrownBy(() -> engine("same", GatewayEngineRoleEnum.MCP, 18084, 18084))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("must differ");
        for (String endpoint : List.of("relative", "http://user:secret@localhost:18084", "http://localhost",
                "http://localhost:18084/path", "http://localhost:18084?token=secret")) {
            assertThatThrownBy(() -> GatewayProcessSpec.engineBuilder("mcp", GatewayEngineRoleEnum.MCP,
                    URI.create(endpoint), URI.create("http://localhost:18085")).build())
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("dataPlaneBaseUri");
        }
        var api = engine("api", GatewayEngineRoleEnum.API_RPC, 18081, 18083);
        var mcp = engine("mcp", GatewayEngineRoleEnum.MCP, 18081, 18085);
        assertThatThrownBy(() -> GatewayProcessHarness.validateProcessIsolation(mcp, List.of(api)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("collides");
        assertThatThrownBy(() -> GatewayProcessHarness.validateProcessIsolation(api, List.of(api)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("already running");
        var sharedDirectory = GatewayProcessSpec.engineBuilder("other", GatewayEngineRoleEnum.MCP,
                URI.create("http://127.0.0.1:18084"), URI.create("http://127.0.0.1:18085"))
                .argument("egon.cola.component.gateway.mcp-engine.data-directory",
                        GatewayProcessHarness.runtimeDataDirectory(api, null)).build();
        assertThatThrownBy(() -> GatewayProcessHarness.validateProcessIsolation(sharedDirectory, List.of(api)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("LKG directory");
    }

    @Test
    void resolvesOnlyOneMatchingRoleExecutableAndNeverFallsBackToApiClasspath() throws Exception {
        Path classes = Files.createDirectories(temporaryDirectory.resolve("target/classes"));
        var api = engine("api", GatewayEngineRoleEnum.API_RPC, 18081, 18083);
        var mcp = engine("mcp", GatewayEngineRoleEnum.MCP, 18084, 18085);
        writeExecutable(classes.getParent().resolve(api.artifactId() + "-exec.jar"), api.mainClass());
        assertThatThrownBy(() -> GatewayProcessHarness.resolveJar(mcp, classes))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("MCP").hasMessageContaining("found 0");
        writeExecutable(classes.getParent().resolve(mcp.artifactId() + "-wrong-exec.jar"), api.mainClass());
        assertThatThrownBy(() -> GatewayProcessHarness.resolveJar(mcp, classes))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("found 0");
        Path valid = classes.getParent().resolve(mcp.artifactId() + "-exec.jar");
        writeExecutable(valid, mcp.mainClass());
        assertThat(GatewayProcessHarness.resolveJar(mcp, classes)).isEqualTo(valid);
        writeExecutable(classes.getParent().resolve(mcp.artifactId() + "-duplicate-exec.jar"), mcp.mainClass());
        assertThatThrownBy(() -> GatewayProcessHarness.resolveJar(mcp, classes))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("found 2");
    }

    private GatewayProcessSpec engine(String name, GatewayEngineRoleEnum role, int dataPort, int managementPort) {
        return GatewayProcessSpec.engineBuilder(name, role, URI.create("http://127.0.0.1:" + dataPort),
                URI.create("http://127.0.0.1:" + managementPort))
                .argument("egon.cola.component.gateway." + (role == GatewayEngineRoleEnum.MCP ? "mcp-engine" : "engine")
                        + ".data-directory", temporaryDirectory.resolve(name)).build();
    }

    private void writeExecutable(Path path, String mainClass) throws Exception {
        var manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue("Main-Class", "org.springframework.boot.loader.launch.JarLauncher");
        manifest.getMainAttributes().putValue("Start-Class", mainClass);
        try (var jar = new JarOutputStream(Files.newOutputStream(path), manifest)) {
            jar.flush();
        }
    }

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
