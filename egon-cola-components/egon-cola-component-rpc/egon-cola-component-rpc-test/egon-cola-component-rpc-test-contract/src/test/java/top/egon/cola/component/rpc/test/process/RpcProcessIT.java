package top.egon.cola.component.rpc.test.process;

import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import top.egon.cola.component.ddc.autoconfigure.DdcProperties;
import top.egon.cola.component.ddc.registry.model.DdcServiceKind;
import top.egon.cola.component.ddc.registry.model.DdcServiceInstance;
import top.egon.cola.component.ddc.registry.model.DdcServiceKey;
import top.egon.cola.component.ddc.registry.client.HttpDdcServiceRegistryClient;
import top.egon.cola.component.ddc.registry.DdcServiceRegistryClient;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class RpcProcessIT {

    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(45);

    private static final Duration DEREGISTRATION_TIMEOUT =
            Duration.ofSeconds(10);

    @Test
    void shouldCallProviderThroughMockGatewayInIndependentProcesses()
            throws Exception {
        String redisHost = requiredEnvironment("DDC_TEST_REDIS_HOST");
        int redisPort = Integer.parseInt(
                requiredEnvironment("DDC_TEST_REDIS_PORT")
        );
        String redisPassword = System.getenv("DDC_TEST_REDIS_PASSWORD");
        String scope = UUID.randomUUID().toString().replace("-", "");
        String env = "process-" + scope;
        String namespace = "rpc-" + scope;
        int adminPort = RpcProcessHarness.availablePort();
        int providerPort = RpcProcessHarness.availablePort();
        int gatewayPort = RpcProcessHarness.availablePort();
        Path database = Path.of(
                System.getProperty(
                        "rpc.process.output.directory",
                        "target/rpc-process-it"
                ),
                "ddc-" + scope + ".db"
        ).toAbsolutePath();
        String adminEndpoint = "http://127.0.0.1:" + adminPort;

        try (RpcProcessHarness processes = new RpcProcessHarness(scope)) {
            RpcProcessHarness.Child admin = processes.start(
                    "admin",
                    "top.egon.cola.component.ddc.admin."
                            + "DynamicConfigCenterAdminApplication",
                    adminArguments(
                            adminPort,
                            redisHost,
                            redisPort,
                            database
                    )
            );
            processes.awaitHttp(
                    adminEndpoint + "/actuator/health/readiness",
                    STARTUP_TIMEOUT,
                    admin
            );

            try (RegistryResources registry = registry(
                    adminEndpoint,
                    redisHost,
                    redisPort,
                    env,
                    namespace
            )) {
                RpcProcessHarness.Child provider = processes.start(
                        "provider",
                        "top.egon.cola.component.rpc.test.fixture.provider."
                                + "RpcTestProviderApplication",
                        rpcArguments(
                                adminEndpoint,
                                redisHost,
                                redisPort,
                                env,
                                namespace,
                                List.of(
                                        "--egon.cola.component.rpc.provider.enabled=true",
                                        "--egon.cola.component.rpc.provider.bind-address=127.0.0.1",
                                        "--egon.cola.component.rpc.provider.port="
                                                + providerPort,
                                        "--egon.cola.component.rpc.provider.advertised-host=127.0.0.1",
                                        "--rpc.test.provider-id=process-provider"
                                )
                        )
                );
                DdcServiceKey providerKey = serviceKey(
                        env,
                        DdcServiceKind.RPC_PROVIDER,
                        "egon.rpc.test.v1.EchoService"
                );
                processes.awaitCondition(
                        () -> registry.client()
                                .getInstances(providerKey)
                                .instances()
                                .size() == 1,
                        STARTUP_TIMEOUT,
                        provider
                );
                DdcServiceInstance providerLease = registry.client()
                        .getInstances(providerKey)
                        .instances()
                        .getFirst();
                assertThat(providerLease.leaseSeconds()).isEqualTo(60);
                assertThat(providerLease.heartbeatIntervalSeconds())
                        .isEqualTo(10);

                java.util.ArrayList<String> gatewayArguments =
                        new java.util.ArrayList<>(List.of(
                                "--ddc.endpoint=" + adminEndpoint,
                                "--ddc.redis.host=" + redisHost,
                                "--ddc.redis.port=" + redisPort,
                                "--ddc.env=" + env,
                                "--ddc.namespace=" + namespace,
                                "--gateway.port=" + gatewayPort
                        ));
                addPassword(
                        gatewayArguments,
                        "--ddc.redis.password=",
                        redisPassword
                );
                RpcProcessHarness.Child gateway = processes.start(
                        "gateway",
                        RpcMockGatewayApplication.class.getName(),
                        gatewayArguments
                );
                DdcServiceKey gatewayKey = serviceKey(
                        env,
                        DdcServiceKind.INTERNAL_GATEWAY,
                        "egon-internal-rpc-gateway"
                );
                processes.awaitCondition(
                        () -> registry.client()
                                .getInstances(gatewayKey)
                                .instances()
                                .size() == 1,
                        STARTUP_TIMEOUT,
                        gateway
                );
                DdcServiceInstance gatewayLease = registry.client()
                        .getInstances(gatewayKey)
                        .instances()
                        .getFirst();
                assertThat(gatewayLease.leaseSeconds()).isEqualTo(15);
                assertThat(gatewayLease.heartbeatIntervalSeconds())
                        .isEqualTo(3);

                RpcProcessHarness.Child consumer = processes.start(
                        "consumer",
                        "top.egon.cola.component.rpc.test.fixture.consumer."
                                + "RpcTestConsumerApplication",
                        rpcArguments(
                                adminEndpoint,
                                redisHost,
                                redisPort,
                                env,
                                namespace,
                                List.of(
                                        "--egon.cola.component.rpc.consumer.enabled=true",
                                        "--rpc.test.run-once=true",
                                        "--rpc.test.message=process-call"
                                )
                        )
                );
                assertThat(List.of(
                        admin.process().pid(),
                        provider.process().pid(),
                        gateway.process().pid(),
                        consumer.process().pid()
                ).stream().distinct()).hasSize(4);
                assertThat(processes.awaitExit(
                        consumer,
                        STARTUP_TIMEOUT
                )).isZero();
                String consumerOutput = processes.output(consumer);
                String invocationId = value(
                        consumerOutput,
                        "invocationId"
                );
                String traceId = value(consumerOutput, "traceId");
                assertThat(consumerOutput)
                        .contains("RPC_PROCESS_SUCCESS")
                        .contains("providerId=process-provider")
                        .contains("message=process-call");
                assertThat(invocationId).isNotBlank();
                assertThat(traceId).matches("[0-9a-f]{32}");
                assertThat(processes.output(gateway))
                        .contains("RPC_MOCK_GATEWAY_FORWARD")
                        .contains("invocationId=" + invocationId)
                        .contains("providerId=" + providerLease.instanceId())
                        .doesNotContain("invocationId=missing");
                assertThat(processes.output(provider))
                        .contains("RPC_PROCESS_PROVIDER")
                        .contains("invocationId=" + invocationId)
                        .contains("providerId=process-provider");

                processes.stop(provider);
                processes.awaitCondition(
                        () -> registry.client()
                                .getInstances(providerKey)
                                .instances()
                                .stream()
                                .noneMatch(instance ->
                                        sameLease(instance, providerLease)),
                        DEREGISTRATION_TIMEOUT,
                        "exact Provider lease deregistration"
                );
                assertThat(registry.client()
                        .getInstances(gatewayKey)
                        .instances()).hasSize(1);
                processes.stop(gateway);
                processes.awaitCondition(
                        () -> registry.client()
                                .getInstances(gatewayKey)
                                .instances()
                                .stream()
                                .noneMatch(instance ->
                                        sameLease(instance, gatewayLease)),
                        DEREGISTRATION_TIMEOUT,
                        "exact Gateway lease deregistration"
                );
            }

            assertMigrations(database);
            assertThat(processes.outputs())
                    .doesNotContain("secret-key")
                    .doesNotContain("DDC_TEST_REDIS_PASSWORD");
            if (redisPassword != null && !redisPassword.isBlank()) {
                assertThat(processes.outputs())
                        .doesNotContain(redisPassword);
            }
        }
    }

    private List<String> adminArguments(
            int adminPort,
            String redisHost,
            int redisPort,
            Path database) {
        java.util.ArrayList<String> arguments =
                new java.util.ArrayList<>(List.of(
                "--server.address=127.0.0.1",
                "--server.port=" + adminPort,
                "--spring.datasource.url=jdbc:sqlite:" + database,
                "--spring.datasource.driver-class-name=org.sqlite.JDBC",
                "--spring.datasource.hikari.maximum-pool-size=2",
                "--spring.jpa.database-platform="
                        + "org.hibernate.community.dialect.SQLiteDialect",
                "--spring.jpa.hibernate.ddl-auto=none",
                "--spring.flyway.enabled=true",
                "--spring.flyway.locations=classpath:db/sqlite",
                "--egon.cola.component.ddc.enabled=false",
                "--egon.cola.component.ddc.admin.redis.host=" + redisHost,
                "--egon.cola.component.ddc.admin.redis.port=" + redisPort,
                "--egon.cola.component.ddc.admin.openapi.signature-enabled=false"
        ));
        addPassword(
                arguments,
                "--egon.cola.component.ddc.admin.redis.password=",
                System.getenv("DDC_TEST_REDIS_PASSWORD")
        );
        return List.copyOf(arguments);
    }

    private List<String> rpcArguments(
            String adminEndpoint,
            String redisHost,
            int redisPort,
            String env,
            String namespace,
            List<String> roleArguments) {
        java.util.ArrayList<String> arguments = new java.util.ArrayList<>(
                List.of(
                        "--spring.main.web-application-type=none",
                        "--egon.cola.component.ddc.enabled=false",
                        "--egon.cola.component.ddc.registry.enabled=true",
                        "--egon.cola.component.ddc.admin.endpoint="
                                + adminEndpoint,
                        "--egon.cola.component.ddc.admin.tls."
                                + "development-plaintext=true",
                        "--egon.cola.component.ddc.redis.host=" + redisHost,
                        "--egon.cola.component.ddc.redis.port=" + redisPort,
                        "--egon.cola.component.ddc.env=" + env,
                        "--egon.cola.component.ddc.namespace=" + namespace,
                        "--egon.cola.component.rpc.enabled=true",
                        "--egon.cola.component.rpc.tls.development-plaintext=true",
                        "--egon.cola.component.rpc.provider.lease-seconds=60",
                        "--egon.cola.component.rpc.provider.heartbeat-interval-seconds=10",
                        "--egon.cola.component.rpc.consumer.gateway-discovery-timeout-ms=15000",
                        "--egon.rpc.runtime-version=process-it"
                )
        );
        arguments.addAll(roleArguments);
        addPassword(
                arguments,
                "--egon.cola.component.ddc.redis.password=",
                System.getenv("DDC_TEST_REDIS_PASSWORD")
        );
        return List.copyOf(arguments);
    }

    private DdcServiceKey serviceKey(
            String env,
            DdcServiceKind kind,
            String serviceName) {
        return new DdcServiceKey(
                "test-biz",
                env,
                "test-app",
                kind,
                serviceName,
                "default",
                "1.0.0",
                "grpc"
        );
    }

    private RegistryResources registry(
            String adminEndpoint,
            String redisHost,
            int redisPort,
            String env,
            String namespace) {
        DdcProperties properties = new DdcProperties();
        properties.getAdmin().setEndpoint(adminEndpoint);
        properties.getRedis().setHost(redisHost);
        properties.getRedis().setPort(redisPort);
        properties.setEnv(env);
        properties.setNamespace(namespace);
        properties.getRegistry().setReconcileIntervalSeconds(1);
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort);
        String password = System.getenv("DDC_TEST_REDIS_PASSWORD");
        if (password != null && !password.isBlank()) {
            config.useSingleServer().setPassword(password);
            properties.getRedis().setPassword(password);
        }
        RedissonClient redisson = Redisson.create(config);
        return new RegistryResources(
                new HttpDdcServiceRegistryClient(properties, redisson),
                redisson
        );
    }

    private void assertMigrations(Path database) throws Exception {
        try (var connection = DriverManager.getConnection(
                "jdbc:sqlite:" + database
        ); var statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM flyway_schema_history "
                        + "WHERE success = 1 AND version IN ('1', '2')"
        ); var result = statement.executeQuery()) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(2);
        }
    }

    private boolean sameLease(
            DdcServiceInstance left,
            DdcServiceInstance right) {
        return left.instanceId().equals(right.instanceId())
                && left.leaseId().equals(right.leaseId());
    }

    private String value(String output, String name) {
        Matcher matcher = Pattern.compile(
                "(?:^|\\s)" + Pattern.quote(name) + "=([^\\s]+)"
        ).matcher(output);
        assertThat(matcher.find())
                .as("process output contains %s", name)
                .isTrue();
        return matcher.group(1);
    }

    private String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    name + " is required by the ddc-live-test profile"
            );
        }
        return value;
    }

    private void addPassword(
            List<String> arguments,
            String prefix,
            String password) {
        if (password != null && !password.isBlank()) {
            arguments.add(prefix + password);
        }
    }

    private record RegistryResources(
            DdcServiceRegistryClient client,
            RedissonClient redisson
    ) implements AutoCloseable {

        @Override
        public void close() throws Exception {
            if (client instanceof AutoCloseable closeable) {
                closeable.close();
            }
            redisson.shutdown();
        }
    }
}
