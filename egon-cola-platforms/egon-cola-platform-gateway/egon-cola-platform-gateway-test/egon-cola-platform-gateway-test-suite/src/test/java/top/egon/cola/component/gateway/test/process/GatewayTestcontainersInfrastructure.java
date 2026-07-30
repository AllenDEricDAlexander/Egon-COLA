package top.egon.cola.component.gateway.test.process;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

final class GatewayTestcontainersInfrastructure
        implements GatewayInfrastructureBackend {

    private boolean started;

    private boolean closed;

    private final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16.6-alpine");

    private final GenericContainer<?> ddcRedis =
            new GenericContainer<>("redis:7.4-alpine")
                    .withExposedPorts(6379);

    private final GenericContainer<?> rateLimitRedis =
            new GenericContainer<>("redis:7.4-alpine")
                    .withExposedPorts(6379);

    private final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka-native:3.9.1")
    );

    @Override
    public String type() {
        return "testcontainers";
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
            postgres.start();
            ddcRedis.start();
            rateLimitRedis.start();
            kafka.start();
            started = true;
        } catch (RuntimeException failure) {
            close();
            throw failure;
        }
    }

    @Override
    public void createDatabase(String database) {
        validateDatabase(database);
        try {
            var result = postgres.execInContainer(
                    "psql",
                    "-U",
                    postgres.getUsername(),
                    "-d",
                    postgres.getDatabaseName(),
                    "-v",
                    "ON_ERROR_STOP=1",
                    "-c",
                    "CREATE DATABASE " + database
            );
            if (result.getExitCode() != 0) {
                throw new IllegalStateException(
                        "cannot create database "
                                + database
                                + ": "
                                + result.getStderr()
                );
            }
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot create database " + database,
                    failure
            );
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "database creation interrupted",
                    interrupted
            );
        }
    }

    @Override
    public String jdbcUrl(String database) {
        validateDatabase(database);
        return "jdbc:postgresql://"
                + postgres.getHost()
                + ":"
                + postgres.getMappedPort(
                        PostgreSQLContainer.POSTGRESQL_PORT
                )
                + "/"
                + database;
    }

    @Override
    public String postgresUsername() {
        return postgres.getUsername();
    }

    @Override
    public String postgresPassword() {
        return postgres.getPassword();
    }

    @Override
    public String ddcRedisHost() {
        return ddcRedis.getHost();
    }

    @Override
    public int ddcRedisPort() {
        return ddcRedis.getMappedPort(6379);
    }

    @Override
    public String rateLimitRedisHost() {
        return rateLimitRedis.getHost();
    }

    @Override
    public int rateLimitRedisPort() {
        return rateLimitRedis.getMappedPort(6379);
    }

    @Override
    public String kafkaBootstrapServers() {
        return kafka.getBootstrapServers();
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        stopIfRunning(kafka);
        stopIfRunning(rateLimitRedis);
        stopIfRunning(ddcRedis);
        stopIfRunning(postgres);
        started = false;
    }

    private void stopIfRunning(GenericContainer<?> container) {
        if (container.isRunning()) {
            container.stop();
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
