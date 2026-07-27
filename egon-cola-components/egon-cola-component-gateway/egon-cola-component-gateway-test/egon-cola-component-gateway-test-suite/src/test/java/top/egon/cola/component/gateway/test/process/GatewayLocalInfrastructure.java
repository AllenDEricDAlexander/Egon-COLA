package top.egon.cola.component.gateway.test.process;

import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

final class GatewayLocalInfrastructure
        implements GatewayInfrastructureBackend {

    private static final String HOST = "127.0.0.1";

    private static final String POSTGRES_USER = "gateway_live";

    private static final String POSTGRES_PASSWORD = "";

    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(30);

    private Path rootDirectory;

    private Process postgres;

    private Process ddcRedis;

    private Process rateLimitRedis;

    private EmbeddedKafkaBroker kafka;

    private int postgresPort;

    private int ddcRedisPort;

    private int rateLimitRedisPort;

    private boolean started;

    private boolean closed;

    @Override
    public String type() {
        return "local";
    }

    @Override
    public synchronized void start() {
        if (closed) {
            throw new IllegalStateException(
                    "gateway test infrastructure is already closed"
            );
        }
        if (started) {
            throw new IllegalStateException(
                    "gateway test infrastructure is already started"
            );
        }
        try {
            rootDirectory = Files.createTempDirectory(
                    "egon-gateway-live-infrastructure-"
            );
            startPostgres();
            ddcRedisPort = GatewayProcessHarness.availablePort();
            ddcRedis = startRedis("ddc-redis", ddcRedisPort);
            awaitRedis(ddcRedis, ddcRedisPort);
            rateLimitRedisPort = GatewayProcessHarness.availablePort();
            rateLimitRedis = startRedis(
                    "rate-limit-redis",
                    rateLimitRedisPort
            );
            awaitRedis(rateLimitRedis, rateLimitRedisPort);
            startKafka();
            started = true;
        } catch (IOException | RuntimeException failure) {
            close();
            throw new IllegalStateException(
                    "cannot start local gateway test infrastructure",
                    failure
            );
        }
    }

    @Override
    public void createDatabase(String database) {
        validateDatabase(database);
        ensureStarted();
        try (var connection = DriverManager.getConnection(
                jdbcUrl("postgres"),
                POSTGRES_USER,
                POSTGRES_PASSWORD
        ); var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + database);
        } catch (SQLException failure) {
            throw new IllegalStateException(
                    "cannot create database " + database,
                    failure
            );
        }
    }

    @Override
    public String jdbcUrl(String database) {
        validateDatabase(database);
        return "jdbc:postgresql://"
                + HOST
                + ":"
                + postgresPort
                + "/"
                + database;
    }

    @Override
    public String postgresUsername() {
        return POSTGRES_USER;
    }

    @Override
    public String postgresPassword() {
        return POSTGRES_PASSWORD;
    }

    @Override
    public String ddcRedisHost() {
        return HOST;
    }

    @Override
    public int ddcRedisPort() {
        return ddcRedisPort;
    }

    @Override
    public String rateLimitRedisHost() {
        return HOST;
    }

    @Override
    public int rateLimitRedisPort() {
        return rateLimitRedisPort;
    }

    @Override
    public String kafkaBootstrapServers() {
        ensureStarted();
        return kafka.getBrokersAsString();
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (kafka != null) {
            kafka.destroy();
        }
        stop(rateLimitRedis);
        stop(ddcRedis);
        stop(postgres);
        deleteRootDirectory();
        started = false;
    }

    private void startPostgres() throws IOException {
        Path dataDirectory = Files.createDirectory(
                rootDirectory.resolve("postgres-data")
        );
        Path initLog = rootDirectory.resolve("postgres-init.log");
        runToCompletion(
                List.of(
                        executable("initdb"),
                        "-D",
                        dataDirectory.toString(),
                        "-U",
                        POSTGRES_USER,
                        "--auth=trust",
                        "--no-locale",
                        "--encoding=UTF8"
                ),
                initLog,
                "initdb"
        );
        postgresPort = GatewayProcessHarness.availablePort();
        postgres = startProcess(
                List.of(
                        executable("postgres"),
                        "-D",
                        dataDirectory.toString(),
                        "-h",
                        HOST,
                        "-p",
                        Integer.toString(postgresPort),
                        "-c",
                        "unix_socket_directories="
                ),
                rootDirectory.resolve("postgres.log")
        );
        awaitPostgres();
    }

    private Process startRedis(String name, int port) throws IOException {
        Path dataDirectory = Files.createDirectory(
                rootDirectory.resolve(name)
        );
        return startProcess(
                List.of(
                        executable("redis-server"),
                        "--bind",
                        HOST,
                        "--port",
                        Integer.toString(port),
                        "--protected-mode",
                        "yes",
                        "--save",
                        "",
                        "--appendonly",
                        "no",
                        "--dir",
                        dataDirectory.toString(),
                        "--daemonize",
                        "no"
                ),
                rootDirectory.resolve(name + ".log")
        );
    }

    private void startKafka() {
        kafka = new EmbeddedKafkaKraftBroker(
                1,
                1,
                "egon.gateway.call.v1"
        ).brokerProperties(Map.of(
                "auto.create.topics.enable",
                "true",
                "delete.topic.enable",
                "true"
        ));
        kafka.afterPropertiesSet();
    }

    private void awaitPostgres() {
        Instant deadline = Instant.now().plus(STARTUP_TIMEOUT);
        SQLException lastFailure = null;
        while (Instant.now().isBefore(deadline)) {
            ensureAlive(postgres, "PostgreSQL");
            try (var ignored = DriverManager.getConnection(
                    jdbcUrl("postgres"),
                    POSTGRES_USER,
                    POSTGRES_PASSWORD
            )) {
                return;
            } catch (SQLException failure) {
                lastFailure = failure;
                pause();
            }
        }
        throw new IllegalStateException(
                "PostgreSQL did not become ready",
                lastFailure
        );
    }

    private void awaitRedis(Process process, int port) {
        Instant deadline = Instant.now().plus(STARTUP_TIMEOUT);
        IOException lastFailure = null;
        while (Instant.now().isBefore(deadline)) {
            ensureAlive(process, "Redis");
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(HOST, port), 500);
                socket.setSoTimeout(500);
                OutputStream output = socket.getOutputStream();
                output.write("*1\r\n$4\r\nPING\r\n".getBytes(
                        StandardCharsets.US_ASCII
                ));
                output.flush();
                InputStream input = socket.getInputStream();
                byte[] response = input.readNBytes(7);
                if ("+PONG\r\n".equals(new String(
                        response,
                        StandardCharsets.US_ASCII
                ))) {
                    return;
                }
            } catch (IOException failure) {
                lastFailure = failure;
                pause();
            }
        }
        throw new IllegalStateException(
                "Redis did not become ready on port " + port,
                lastFailure
        );
    }

    private Process startProcess(List<String> command, Path log)
            throws IOException {
        return new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(log.toFile())
                .start();
    }

    private void runToCompletion(
            List<String> command,
            Path log,
            String description) throws IOException {
        Process process = startProcess(command, log);
        try {
            if (!process.waitFor(STARTUP_TIMEOUT.toSeconds(),
                    java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException(
                        description + " timed out; see " + log
                );
            }
            if (process.exitValue() != 0) {
                throw new IllegalStateException(
                        description
                                + " exited with "
                                + process.exitValue()
                                + "; see "
                                + log
                );
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IllegalStateException(
                    description + " was interrupted",
                    interrupted
            );
        }
    }

    private String executable(String name) {
        String path = System.getenv("PATH");
        if (path != null) {
            for (String directory : path.split(
                    java.util.regex.Pattern.quote(
                            java.io.File.pathSeparator
                    )
            )) {
                Path candidate = Path.of(directory).resolve(name);
                if (Files.isExecutable(candidate)) {
                    return candidate.toString();
                }
            }
        }
        throw new IllegalStateException(
                name + " is required on PATH for local live tests"
        );
    }

    private void ensureStarted() {
        if (!started) {
            throw new IllegalStateException(
                    "gateway test infrastructure is not started"
            );
        }
    }

    private void ensureAlive(Process process, String description) {
        if (!process.isAlive()) {
            throw new IllegalStateException(
                    description
                            + " exited with "
                            + process.exitValue()
            );
        }
    }

    private void pause() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "local infrastructure readiness was interrupted",
                    interrupted
            );
        }
    }

    private void stop(Process process) {
        if (process == null || !process.isAlive()) {
            return;
        }
        process.destroy();
        try {
            if (!process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(
                        10,
                        java.util.concurrent.TimeUnit.SECONDS
                );
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }

    private void deleteRootDirectory() {
        if (rootDirectory == null || !Files.exists(rootDirectory)) {
            return;
        }
        try (var paths = Files.walk(rootDirectory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException failure) {
                    throw new IllegalStateException(
                            "cannot delete local infrastructure path "
                                    + path,
                            failure
                    );
                }
            });
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot clean local infrastructure directory",
                    failure
            );
        }
    }

    private void validateDatabase(String database) {
        if (database == null || !database.matches("[a-z][a-z0-9_]{0,62}")) {
            throw new IllegalArgumentException(
                    "database must be a safe PostgreSQL identifier"
            );
        }
    }
}
