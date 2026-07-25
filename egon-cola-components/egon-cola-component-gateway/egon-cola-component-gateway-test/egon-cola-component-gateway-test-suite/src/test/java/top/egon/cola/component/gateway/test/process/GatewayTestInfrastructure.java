package top.egon.cola.component.gateway.test.process;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

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
