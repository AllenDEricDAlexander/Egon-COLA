package top.egon.cola.component.gateway.test.process;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

public final class GatewayTestInfrastructure implements AutoCloseable {

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

    public void start() {
        postgres.start();
        ddcRedis.start();
        rateLimitRedis.start();
        kafka.start();
    }

    public PostgreSQLContainer<?> postgres() {
        return postgres;
    }

    public void createDatabase(String database) {
        if (database == null || !database.matches("[a-z][a-z0-9_]{0,62}")) {
            throw new IllegalArgumentException(
                    "database must be a safe PostgreSQL identifier"
            );
        }
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

    public String jdbcUrl(String database) {
        if (database == null || !database.matches("[a-z][a-z0-9_]{0,62}")) {
            throw new IllegalArgumentException(
                    "database must be a safe PostgreSQL identifier"
            );
        }
        return "jdbc:postgresql://"
                + postgres.getHost()
                + ":"
                + postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)
                + "/"
                + database;
    }

    public String ddcRedisHost() {
        return ddcRedis.getHost();
    }

    public int ddcRedisPort() {
        return ddcRedis.getMappedPort(6379);
    }

    public String rateLimitRedisHost() {
        return rateLimitRedis.getHost();
    }

    public int rateLimitRedisPort() {
        return rateLimitRedis.getMappedPort(6379);
    }

    public String kafkaBootstrapServers() {
        return kafka.getBootstrapServers();
    }

    @Override
    public void close() {
        kafka.stop();
        rateLimitRedis.stop();
        ddcRedis.stop();
        postgres.stop();
    }
}
