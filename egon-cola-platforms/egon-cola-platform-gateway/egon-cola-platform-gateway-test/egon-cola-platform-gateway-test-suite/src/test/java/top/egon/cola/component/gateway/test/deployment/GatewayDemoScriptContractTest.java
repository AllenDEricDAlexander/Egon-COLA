package top.egon.cola.component.gateway.test.deployment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayDemoScriptContractTest {

    private static final Path DEPLOYMENT = projectFile(Path.of(
            "egon-cola-platforms",
            "egon-cola-platform-gateway",
            "deployment"
    ));

    @TempDir
    private Path temporaryDirectory;

    @Test
    void helpPublishesTheCompleteOperatorCommandSurface() throws Exception {
        ProcessResult result = run("--help", false);

        assertThat(result.exitCode()).isZero();
        assertThat(result.output()).contains(
                "doctor",
                "build",
                "up-control",
                "init",
                "up-providers",
                "publish",
                "up-consumer",
                "verify",
                "logs",
                "down",
                "purge"
        );
    }

    @Test
    void downPreservesVolumesAndPurgeRequiresLocalMarker()
            throws Exception {
        Path commandLog = temporaryDirectory.resolve("docker.log");
        ProcessResult down = run("down", true, commandLog);

        assertThat(down.exitCode()).isZero();
        assertThat(Files.readString(commandLog))
                .contains("down --remove-orphans")
                .doesNotContain("--volumes", " -v");

        Files.delete(commandLog);
        ProcessResult refused = run("purge", true, commandLog);
        assertThat(refused.exitCode()).isEqualTo(1);
        assertThat(refused.output()).contains("local demo marker is missing");
        assertThat(commandLog).doesNotExist();

        Path runtime = temporaryDirectory.resolve("runtime");
        Files.createDirectories(runtime);
        Files.createFile(runtime.resolve(".local-demo-marker"));
        ProcessResult purge = run("purge", true, commandLog);
        assertThat(purge.exitCode()).isZero();
        assertThat(Files.readString(commandLog))
                .contains("down --volumes --remove-orphans");
    }

    @Test
    void composeDemoUsesResolvableServiceNamesAndSeparateProviderImages()
            throws IOException {
        Map<String, Object> compose = yaml("compose.demo.yml");
        Map<String, Object> services = map(compose.get("services"));

        assertThat(services).containsKeys(
                "http-provider-mvc",
                "http-provider-webflux",
                "rpc-provider",
                "rpc-consumer"
        );
        assertThat(environment(services, "http-provider-mvc"))
                .containsEntry(
                        "GATEWAY_TEST_ADVERTISED_HOST",
                        "http-provider-mvc"
                );
        assertThat(environment(services, "http-provider-webflux"))
                .containsEntry(
                        "GATEWAY_TEST_ADVERTISED_HOST",
                        "http-provider-webflux"
                );
        assertThat(environment(services, "rpc-provider"))
                .containsEntry(
                        "EGON_COLA_COMPONENT_RPC_PROVIDER_ADVERTISED_HOST",
                        "rpc-provider"
                );
    }

    @Test
    void tokenScriptSupportsSecretsWithRepeatedByteLines() throws Exception {
        String secret = Base64.getEncoder().encodeToString(
                "0123456789abcdef0123456789abcdef"
                        .getBytes(StandardCharsets.UTF_8)
        );
        Process process = new ProcessBuilder(
                "bash",
                DEPLOYMENT.resolve("scripts/demo-token.sh").toString(),
                secret
        ).redirectErrorStream(true).start();
        String output = new String(
                process.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        ).trim();

        assertThat(process.waitFor()).isZero();
        assertThat(output).matches("[^.]+\\.[^.]+\\.[^.]+");
    }

    private ProcessResult run(String command, boolean fakeDocker)
            throws Exception {
        return run(
                command,
                fakeDocker,
                temporaryDirectory.resolve("docker.log")
        );
    }

    private ProcessResult run(
            String command,
            boolean fakeDocker,
            Path commandLog) throws Exception {
        Path bin = temporaryDirectory.resolve("bin");
        Files.createDirectories(bin);
        if (fakeDocker) {
            Path docker = bin.resolve("docker");
            Files.writeString(
                    docker,
                    "#!/usr/bin/env bash\nprintf '%s\\n' \"$*\" >>\"$GATEWAY_DEMO_COMMAND_LOG\"\n"
            );
            docker.toFile().setExecutable(true);
        }
        ProcessBuilder builder = new ProcessBuilder(
                "bash",
                DEPLOYMENT.resolve("scripts/demo.sh").toString(),
                command
        ).redirectErrorStream(true);
        Map<String, String> environment = builder.environment();
        environment.put(
                "PATH",
                bin + ":" + environment.getOrDefault("PATH", "")
        );
        environment.put(
                "GATEWAY_DEMO_RUNTIME_DIR",
                temporaryDirectory.resolve("runtime").toString()
        );
        environment.put(
                "GATEWAY_DEMO_ENV_FILE",
                DEPLOYMENT.resolve(".env.example").toString()
        );
        environment.put("GATEWAY_DEMO_PROJECT", "egon-cola-gateway-demo-test");
        environment.put("GATEWAY_DEMO_COMMAND_LOG", commandLog.toString());
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes());
        return new ProcessResult(process.waitFor(), output);
    }

    private Map<String, Object> environment(
            Map<String, Object> services,
            String service) {
        return map(map(services.get(service)).get("environment"));
    }

    private Map<String, Object> yaml(String file) throws IOException {
        try (var input = Files.newInputStream(DEPLOYMENT.resolve(file))) {
            return map(new Yaml().load(input));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    private static Path projectFile(Path projectPath) {
        Path current = Path.of(System.getProperty("user.dir"))
                .toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(projectPath);
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException(
                "cannot locate project directory " + projectPath
        );
    }

    private record ProcessResult(int exitCode, String output) {
    }
}
