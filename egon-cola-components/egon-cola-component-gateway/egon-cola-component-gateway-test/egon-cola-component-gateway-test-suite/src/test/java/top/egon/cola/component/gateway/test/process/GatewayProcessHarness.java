package top.egon.cola.component.gateway.test.process;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class GatewayProcessHarness implements AutoCloseable {

    private static final Duration POLL_INTERVAL = Duration.ofMillis(100);

    private final Path outputDirectory;

    private final List<ChildProcess> children = new ArrayList<>();

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public GatewayProcessHarness(String scope) throws IOException {
        outputDirectory = Path.of(
                System.getProperty(
                        "gateway.process.output.directory",
                        "target/gateway-process-it"
                ),
                scope
        ).toAbsolutePath();
        Files.createDirectories(outputDirectory);
    }

    public static int availablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    public ChildProcess start(GatewayProcessSpec spec) throws IOException {
        Path output = outputDirectory.resolve(spec.name() + ".log");
        Path manifest = outputDirectory.resolve(
                spec.name() + "-manifest.json"
        );
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(
                manifest.toFile(),
                java.util.Map.of(
                        "name", spec.name(),
                        "mainClass", spec.mainClass(),
                        "arguments", spec.redactedArguments(),
                        "environment", spec.redactedEnvironment()
                )
        );
        List<String> command = new ArrayList<>();
        command.add(Path.of(
                System.getProperty("java.home"),
                "bin",
                "java"
        ).toString());
        command.add("-cp");
        command.add(testClassPath());
        command.add(spec.mainClass());
        command.addAll(spec.arguments());
        ProcessBuilder builder = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(output.toFile());
        builder.environment().putAll(spec.environment());
        Process process = builder.start();
        ChildProcess child = new ChildProcess(
                spec.name(),
                process,
                output,
                manifest
        );
        children.add(child);
        return child;
    }

    public void awaitHttp(
            URI uri,
            Duration timeout,
            ChildProcess related) {
        awaitCondition(() -> {
            HttpResponse<Void> response = httpClient.send(
                    HttpRequest.newBuilder(uri)
                            .timeout(Duration.ofSeconds(2))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.discarding()
            );
            return response.statusCode() >= 200
                    && response.statusCode() < 300;
        }, timeout, related.name() + " readiness", related);
    }

    public void awaitCondition(
            CheckedBooleanSupplier condition,
            Duration timeout,
            String description) {
        awaitCondition(condition, timeout, description, null);
    }

    public String output(ChildProcess child) {
        try {
            return Files.exists(child.output())
                    ? Files.readString(child.output())
                    : "";
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot read process log " + child.output(),
                    failure
            );
        }
    }

    public void stop(ChildProcess child) {
        Process process = child.process();
        if (!process.isAlive()) {
            return;
        }
        process.destroy();
        try {
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }

    @Override
    public void close() {
        List<ChildProcess> reverse = new ArrayList<>(children);
        Collections.reverse(reverse);
        reverse.forEach(this::stop);
    }

    private void awaitCondition(
            CheckedBooleanSupplier condition,
            Duration timeout,
            String description,
            ChildProcess related) {
        long deadline = System.nanoTime() + timeout.toNanos();
        Throwable lastFailure = null;
        while (System.nanoTime() < deadline) {
            if (related != null && !related.process().isAlive()) {
                throw new AssertionError(
                        related.name()
                                + " exited before "
                                + description
                                + System.lineSeparator()
                                + output(related)
                );
            }
            try {
                if (condition.getAsBoolean()) {
                    return;
                }
            } catch (Throwable failure) {
                lastFailure = failure;
            }
            awaitPollInterval();
        }
        AssertionError timeoutFailure = new AssertionError(
                "timed out waiting for "
                        + description
                        + (related == null
                        ? ""
                        : System.lineSeparator() + output(related))
        );
        if (lastFailure != null) {
            timeoutFailure.initCause(lastFailure);
        }
        throw timeoutFailure;
    }

    private String testClassPath() {
        return System.getProperty(
                "surefire.test.class.path",
                System.getProperty("java.class.path")
        );
    }

    private void awaitPollInterval() {
        try {
            new CountDownLatch(1).await(
                    POLL_INTERVAL.toMillis(),
                    TimeUnit.MILLISECONDS
            );
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "process readiness wait interrupted",
                    interrupted
            );
        }
    }

    public record ChildProcess(
            String name,
            Process process,
            Path output,
            Path manifest
    ) {
    }

    @FunctionalInterface
    public interface CheckedBooleanSupplier {

        boolean getAsBoolean() throws Exception;
    }
}
