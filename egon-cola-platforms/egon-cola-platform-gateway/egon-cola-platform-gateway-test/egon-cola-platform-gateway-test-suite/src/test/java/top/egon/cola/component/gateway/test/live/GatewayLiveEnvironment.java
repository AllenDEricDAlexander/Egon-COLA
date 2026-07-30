package top.egon.cola.component.gateway.test.live;

import top.egon.cola.component.gateway.test.process.GatewayProcessHarness;
import top.egon.cola.component.gateway.test.process.GatewayProcessSpec;
import top.egon.cola.component.gateway.test.process.GatewayTestInfrastructure;
import top.egon.cola.component.gateway.test.process.GatewayTestScope;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

public final class GatewayLiveEnvironment implements AutoCloseable {

    private final GatewayTestInfrastructure infrastructure;

    private final GatewayProcessHarness processes;

    private final GatewayTestScope scope;

    public GatewayLiveEnvironment(String scenario) throws IOException {
        this(
                scenario,
                Path.of(System.getProperty(
                        "gateway.live.output.directory",
                        "target/gateway-live-it"
                ))
        );
    }

    GatewayLiveEnvironment(
            String scenario,
            Path baseDirectory) throws IOException {
        String safeScenario = safeScenario(scenario);
        Path scenarioDirectory = Objects.requireNonNull(
                baseDirectory,
                "baseDirectory"
        ).toAbsolutePath().resolve(safeScenario);
        scope = GatewayTestScope.create(scenarioDirectory);
        infrastructure = new GatewayTestInfrastructure();
        processes = new GatewayProcessHarness(
                scope.processOutputDirectory()
        );
    }

    public void startInfrastructure() {
        infrastructure.start();
    }

    public GatewayTestInfrastructure infrastructure() {
        return infrastructure;
    }

    public GatewayProcessHarness processes() {
        return processes;
    }

    public GatewayTestScope scope() {
        return scope;
    }

    public Path dataDirectory(String processName) {
        return scope.dataDirectory(processName);
    }

    public Path processOutputDirectory() {
        return scope.processOutputDirectory();
    }

    public GatewayProcessHarness.ChildProcess start(
            GatewayProcessSpec spec) throws IOException {
        return processes.start(spec);
    }

    public void stop(GatewayProcessHarness.ChildProcess process) {
        processes.stop(process);
    }

    public void kill(GatewayProcessHarness.ChildProcess process) {
        processes.kill(process);
    }

    public GatewayProcessHarness.ChildProcess restart(
            GatewayProcessHarness.ChildProcess process) throws IOException {
        return processes.restart(process);
    }

    public void awaitHttp(
            URI uri,
            Duration timeout,
            GatewayProcessHarness.ChildProcess related) {
        processes.awaitHttp(uri, timeout, related);
    }

    public void awaitCondition(
            GatewayProcessHarness.CheckedBooleanSupplier condition,
            Duration timeout,
            String description) {
        processes.awaitCondition(condition, timeout, description);
    }

    @Override
    public void close() {
        try {
            processes.close();
        } finally {
            infrastructure.close();
        }
    }

    private static String safeScenario(String scenario) {
        if (scenario == null
                || !scenario.matches("[A-Za-z0-9][A-Za-z0-9._-]*")) {
            throw new IllegalArgumentException(
                    "scenario must be a safe path segment"
            );
        }
        return scenario;
    }
}
