package top.egon.cola.component.gateway.test.process;

public final class GatewayTestInfrastructure implements AutoCloseable {

    private static final String INFRASTRUCTURE_PROPERTY =
            "gateway.live.infrastructure";

    private final GatewayInfrastructureBackend backend;

    public GatewayTestInfrastructure() {
        this(createBackend(System.getProperty(
                INFRASTRUCTURE_PROPERTY,
                "testcontainers"
        )));
    }

    GatewayTestInfrastructure(GatewayInfrastructureBackend backend) {
        this.backend = java.util.Objects.requireNonNull(
                backend,
                "backend"
        );
    }

    public String type() {
        return backend.type();
    }

    public void start() {
        backend.start();
    }

    public void createDatabase(String database) {
        backend.createDatabase(database);
    }

    public String jdbcUrl(String database) {
        return backend.jdbcUrl(database);
    }

    public String postgresUsername() {
        return backend.postgresUsername();
    }

    public String postgresPassword() {
        return backend.postgresPassword();
    }

    public String ddcRedisHost() {
        return backend.ddcRedisHost();
    }

    public int ddcRedisPort() {
        return backend.ddcRedisPort();
    }

    public String rateLimitRedisHost() {
        return backend.rateLimitRedisHost();
    }

    public int rateLimitRedisPort() {
        return backend.rateLimitRedisPort();
    }

    public String kafkaBootstrapServers() {
        return backend.kafkaBootstrapServers();
    }

    @Override
    public void close() {
        backend.close();
    }

    private static GatewayInfrastructureBackend createBackend(String type) {
        return switch (type.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "testcontainers" ->
                    new GatewayTestcontainersInfrastructure();
            case "local" -> new GatewayLocalInfrastructure();
            default -> throw new IllegalArgumentException(
                    INFRASTRUCTURE_PROPERTY
                            + " must be testcontainers or local"
            );
        };
    }
}
