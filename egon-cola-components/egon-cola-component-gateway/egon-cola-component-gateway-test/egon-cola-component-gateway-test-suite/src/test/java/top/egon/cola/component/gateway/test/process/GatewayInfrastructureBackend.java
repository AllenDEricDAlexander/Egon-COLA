package top.egon.cola.component.gateway.test.process;

interface GatewayInfrastructureBackend extends AutoCloseable {

    String type();

    void start();

    void createDatabase(String database);

    String jdbcUrl(String database);

    String postgresUsername();

    String postgresPassword();

    String ddcRedisHost();

    int ddcRedisPort();

    String rateLimitRedisHost();

    int rateLimitRedisPort();

    String kafkaBootstrapServers();

    @Override
    void close();
}
