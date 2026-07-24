package top.egon.cola.component.rpc.test.process;

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

import static org.assertj.core.api.Assertions.fail;

final class RpcProcessHarness implements AutoCloseable {

    private static final Duration POLL_INTERVAL = Duration.ofMillis(100);

    private final Path outputDirectory;

    private final List<Child> children = new ArrayList<>();

    private final HttpClient httpClient = HttpClient.newHttpClient();

    RpcProcessHarness(String scope) throws IOException {
        outputDirectory = Path.of(
                System.getProperty(
                        "rpc.process.output.directory",
                        "target/rpc-process-it"
                ),
                scope
        ).toAbsolutePath();
        Files.createDirectories(outputDirectory);
    }

    static int availablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    Child start(String name,
                String mainClass,
                List<String> arguments) throws IOException {
        Path output = outputDirectory.resolve(name + ".log");
        List<String> command = new ArrayList<>();
        command.add(Path.of(
                System.getProperty("java.home"),
                "bin",
                "java"
        ).toString());
        command.add("-cp");
        command.add(testClassPath());
        command.add(mainClass);
        command.addAll(arguments);
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(output.toFile())
                .start();
        Child child = new Child(name, process, output);
        children.add(child);
        return child;
    }

    void awaitHttp(String uri,
                   Duration timeout,
                   Child related) {
        awaitCondition(() -> {
            HttpResponse<Void> response = httpClient.send(
                    HttpRequest.newBuilder(URI.create(uri))
                            .timeout(Duration.ofSeconds(2))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.discarding()
            );
            return response.statusCode() >= 200
                    && response.statusCode() < 300;
        }, timeout, related);
    }

    void awaitCondition(
            CheckedBooleanSupplier condition,
            Duration timeout,
            Child related) {
        long deadline = System.nanoTime() + timeout.toNanos();
        Throwable lastFailure = null;
        while (System.nanoTime() < deadline) {
            if (!related.process().isAlive()) {
                fail(
                        related.name() + " exited before readiness:\n"
                                + output(related)
                );
            }
            try {
                if (condition.getAsBoolean()) {
                    return;
                }
            } catch (Throwable exception) {
                lastFailure = exception;
            }
            awaitPollInterval();
        }
        String message = "timed out waiting for " + related.name()
                + ":\n" + output(related);
        if (lastFailure == null) {
            fail(message);
        }
        fail(message, lastFailure);
    }

    int awaitExit(Child child, Duration timeout)
            throws InterruptedException {
        if (!child.process().waitFor(
                timeout.toMillis(),
                TimeUnit.MILLISECONDS
        )) {
            fail("timed out waiting for " + child.name()
                    + ":\n" + output(child));
        }
        return child.process().exitValue();
    }

    void stop(Child child) {
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
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }

    String output(Child child) {
        try {
            return Files.exists(child.output())
                    ? Files.readString(child.output())
                    : "";
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "cannot read process output " + child.output(),
                    exception
            );
        }
    }

    String outputs() {
        StringBuilder all = new StringBuilder();
        children.forEach(child -> all
                .append(System.lineSeparator())
                .append("[").append(child.name()).append("]")
                .append(System.lineSeparator())
                .append(output(child)));
        return all.toString();
    }

    @Override
    public void close() {
        List<Child> reverse = new ArrayList<>(children);
        Collections.reverse(reverse);
        reverse.forEach(this::stop);
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
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "process readiness wait interrupted",
                    exception
            );
        }
    }

    record Child(String name, Process process, Path output) {
    }

    @FunctionalInterface
    interface CheckedBooleanSupplier {

        boolean getAsBoolean() throws Exception;
    }
}
