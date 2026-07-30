package top.egon.cola.component.gateway.test.process;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class GatewayProcessHarness implements AutoCloseable {

    private static final Duration POLL_INTERVAL = Duration.ofMillis(100);

    private static final Duration DEFAULT_GRACEFUL_STOP_TIMEOUT =
            Duration.ofSeconds(10);

    private static final Duration DEFAULT_FORCED_STOP_TIMEOUT =
            Duration.ofSeconds(5);

    private final Path outputDirectory;

    private final Duration gracefulStopTimeout;

    private final Duration forcedStopTimeout;

    private final List<ChildProcess> children = new ArrayList<>();

    private final Map<String, Integer> attempts = new LinkedHashMap<>();

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public GatewayProcessHarness(String scope) throws IOException {
        this(Path.of(
                System.getProperty(
                        "gateway.process.output.directory",
                        "target/gateway-process-it"
                ),
                scope
        ).toAbsolutePath());
    }

    public GatewayProcessHarness(Path outputDirectory) throws IOException {
        this(
                outputDirectory,
                DEFAULT_GRACEFUL_STOP_TIMEOUT,
                DEFAULT_FORCED_STOP_TIMEOUT
        );
    }

    GatewayProcessHarness(
            Path outputDirectory,
            Duration gracefulStopTimeout,
            Duration forcedStopTimeout) throws IOException {
        this.outputDirectory = Objects.requireNonNull(
                outputDirectory,
                "outputDirectory"
        ).toAbsolutePath();
        this.gracefulStopTimeout = positive(
                gracefulStopTimeout,
                "gracefulStopTimeout"
        );
        this.forcedStopTimeout = positive(
                forcedStopTimeout,
                "forcedStopTimeout"
        );
        Files.createDirectories(outputDirectory);
    }

    public static int availablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    public ChildProcess start(GatewayProcessSpec spec) throws IOException {
        Objects.requireNonNull(spec, "spec");
        int attempt = attempts.merge(spec.name(), 1, Integer::sum);
        Path processDirectory = outputDirectory.resolve(spec.name());
        Path attemptDirectory = processDirectory.resolve(
                "attempt-%02d".formatted(attempt)
        );
        Path logFile = attemptDirectory.resolve("process.log");
        Path manifestFile = attemptDirectory.resolve("manifest.json");
        Path lkgDirectory = processDirectory.resolve("lkg");
        Files.createDirectories(attemptDirectory);
        Files.createDirectories(lkgDirectory);
        List<String> command = new ArrayList<>();
        command.add(Path.of(
                System.getProperty("java.home"),
                "bin",
                "java"
        ).toString());
        Path applicationArchive = applicationArchive(spec.mainClass());
        Optional<Path> executableArchive = executableArchive(
                applicationArchive
        );
        if (executableArchive.isPresent()) {
            command.add("-jar");
            command.add(executableArchive.orElseThrow().toString());
        } else {
            command.add("-cp");
            command.add(testClassPath(applicationArchive));
            command.add(spec.mainClass());
        }
        command.addAll(spec.arguments());
        ProcessBuilder builder = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(logFile.toFile());
        builder.environment().putAll(spec.environment());
        Process process = builder.start();
        ChildProcess child = new ChildProcess(
                spec.name(),
                spec,
                attempt,
                process,
                logFile,
                manifestFile,
                lkgDirectory
        );
        try {
            writeManifest(child);
            children.add(child);
        } catch (IOException failure) {
            process.destroyForcibly();
            throw failure;
        }
        return child;
    }

    public ChildProcess restart(ChildProcess child) throws IOException {
        requireOwned(child);
        if (child.process().isAlive()) {
            throw new IllegalStateException(
                    child.name() + " must be stopped before restart"
            );
        }
        return start(child.spec());
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
            return Files.exists(child.logFile())
                    ? Files.readString(child.logFile())
                    : "";
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot read process log " + child.logFile(),
                    failure
            );
        }
    }

    public void stop(ChildProcess child) {
        requireOwned(child);
        Process process = child.process();
        if (!process.isAlive()) {
            return;
        }
        process.destroy();
        try {
            if (!process.waitFor(
                    gracefulStopTimeout.toMillis(),
                    TimeUnit.MILLISECONDS
            )) {
                process.destroyForcibly();
                awaitForcedStop(child);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }

    public void kill(ChildProcess child) {
        requireOwned(child);
        if (!child.process().isAlive()) {
            return;
        }
        child.process().destroyForcibly();
        try {
            awaitForcedStop(child);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "process kill interrupted for " + child.name(),
                    interrupted
            );
        }
    }

    @Override
    public void close() {
        List<ChildProcess> reverse = new ArrayList<>(children);
        Collections.reverse(reverse);
        reverse.forEach(this::stop);
    }

    public void awaitCondition(
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

    private Path applicationArchive(String mainClass) {
        try {
            Class<?> application = Class.forName(
                    mainClass,
                    false,
                    Thread.currentThread().getContextClassLoader()
            );
            var codeSource = application.getProtectionDomain()
                    .getCodeSource();
            if (codeSource == null) {
                throw new IllegalStateException(
                        "child application has no code source: " + mainClass
                );
            }
            return Path.of(codeSource.getLocation().toURI());
        } catch (ClassNotFoundException | URISyntaxException failure) {
            throw new IllegalStateException(
                    "cannot resolve child application archive for "
                            + mainClass,
                    failure
            );
        }
    }

    private String testClassPath(Path applicationArchive) {
        String classPath = System.getProperty(
                "surefire.test.class.path",
                System.getProperty("java.class.path")
        );
        return prioritizeClassPath(classPath, applicationArchive);
    }

    static Optional<Path> executableArchive(Path applicationArchive) {
        Path fileName = applicationArchive.getFileName();
        if (fileName == null || !Files.isRegularFile(applicationArchive)) {
            return Optional.empty();
        }
        String name = fileName.toString();
        if (!name.endsWith(".jar")) {
            return Optional.empty();
        }
        Path executable = applicationArchive.resolveSibling(
                name.substring(0, name.length() - ".jar".length())
                        + "-exec.jar"
        );
        return Files.isRegularFile(executable)
                ? Optional.of(executable)
                : Optional.empty();
    }

    static String prioritizeClassPath(
            String classPath,
            Path preferredEntry) {
        List<String> entries = new ArrayList<>(List.of(classPath.split(
                java.util.regex.Pattern.quote(File.pathSeparator)
        )));
        Path normalizedPreferred = preferredEntry.toAbsolutePath()
                .normalize();
        int preferredIndex = -1;
        for (int index = 0; index < entries.size(); index++) {
            Path candidate = Path.of(entries.get(index))
                    .toAbsolutePath()
                    .normalize();
            if (candidate.equals(normalizedPreferred)) {
                preferredIndex = index;
                break;
            }
        }
        if (preferredIndex <= 0) {
            return classPath;
        }
        String preferred = entries.remove(preferredIndex);
        entries.addFirst(preferred);
        return String.join(File.pathSeparator, entries);
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

    private void awaitForcedStop(ChildProcess child)
            throws InterruptedException {
        if (!child.process().waitFor(
                forcedStopTimeout.toMillis(),
                TimeUnit.MILLISECONDS
        )) {
            throw new IllegalStateException(
                    "process did not stop after forced termination: "
                            + child.name()
                            + System.lineSeparator()
                            + output(child)
            );
        }
    }

    private void writeManifest(ChildProcess child) throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(
                child.manifestFile().toFile(),
                Map.of(
                        "name", child.name(),
                        "attempt", child.attempt(),
                        "pid", child.process().pid(),
                        "mainClass", child.spec().mainClass(),
                        "arguments", child.spec().redactedArguments(),
                        "environment", child.spec().redactedEnvironment(),
                        "logFile", child.logFile().toString(),
                        "lkgDirectory", child.lkgDirectory().toString()
                )
        );
    }

    private void requireOwned(ChildProcess child) {
        Objects.requireNonNull(child, "child");
        if (!children.contains(child)) {
            throw new IllegalArgumentException(
                    "process is not owned by this harness: " + child.name()
            );
        }
    }

    private static Duration positive(Duration value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    public record ChildProcess(
            String name,
            GatewayProcessSpec spec,
            int attempt,
            Process process,
            Path logFile,
            Path manifestFile,
            Path lkgDirectory
    ) {

        public Path output() {
            return logFile;
        }

        public Path manifest() {
            return manifestFile;
        }
    }

    @FunctionalInterface
    public interface CheckedBooleanSupplier {

        boolean getAsBoolean() throws Exception;
    }
}
