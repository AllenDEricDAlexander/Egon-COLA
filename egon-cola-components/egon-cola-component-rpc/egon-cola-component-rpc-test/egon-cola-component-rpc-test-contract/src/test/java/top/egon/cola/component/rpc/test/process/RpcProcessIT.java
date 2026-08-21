package top.egon.cola.component.rpc.test.process;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.ddc.model.registry.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceInstance;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentity;
import top.egon.cola.component.rpc.ddc.autoconfigure.DdcRpcProperties;
import top.egon.cola.component.rpc.ddc.client.DdcRpcClientFactory;
import top.egon.cola.component.rpc.ddc.client.DdcRpcClientHandle;

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
        int adminRpcPort = RpcProcessHarness.availablePort();
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
        String adminTarget = "dns:///127.0.0.1:" + adminRpcPort;

        try (RpcProcessHarness processes = new RpcProcessHarness(scope)) {
            RpcProcessHarness.Child admin = processes.start(
                    "admin",
                    RpcTestDdcAdminApplication.class.getName(),
                    adminArguments(
                            adminPort,
                            adminRpcPort,
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
            seedScope(database, env, namespace);

            try (RegistryResources registry = registry(
                    adminTarget,
                    env,
                    namespace
            )) {
                RpcProcessHarness.Child provider = processes.start(
                        "provider",
                        "top.egon.cola.component.rpc.test.fixture.provider."
                                + "RpcTestProviderApplication",
                        rpcArguments(
                                adminTarget,
                                redisHost,
                                redisPort,
                                env,
                                namespace,
                                List.of(
                                        "--egon.cola.component.rpc.provider.enabled=true",
                                        "--egon.cola.component.rpc.consumer.enabled=false",
                                        "--egon.cola.component.rpc.provider.registration-mode=REQUIRED",
                                        "--egon.cola.component.rpc.provider.registration-fail-fast=true",
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
                                "--egon.cola.component.id.machine-id=3",
                                "--ddc.target=" + adminTarget,
                                "--ddc.access-key=process-it",
                                "--ddc.secret-key=process-it-secret-at-least-32-bytes",
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
                                adminTarget,
                                redisHost,
                                redisPort,
                                env,
                                namespace,
                                List.of(
                                        "--egon.cola.component.rpc.provider.enabled=false",
                                        "--egon.cola.component.rpc.consumer.enabled=true",
                                        "--egon.cola.component.rpc.consumer.gateway-service-name="
                                                + "egon-internal-rpc-gateway",
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

                RpcProcessHarness.Child directConsumer = processes.start(
                        "direct-consumer",
                        "top.egon.cola.component.rpc.test.fixture.directconsumer."
                                + "RpcDirectTestConsumerApplication",
                        rpcArguments(
                                adminTarget,
                                redisHost,
                                redisPort,
                                env,
                                namespace,
                                List.of(
                                        "--egon.cola.component.rpc.provider.enabled=false",
                                        "--egon.cola.component.rpc.consumer.enabled=true",
                                        "--rpc.test.message=direct-process-call"
                                )
                        )
                );
                assertThat(processes.awaitExit(
                        directConsumer,
                        STARTUP_TIMEOUT
                )).isZero();
                String directOutput = processes.output(directConsumer);
                String directInvocationId = value(
                        directOutput,
                        "invocationId"
                );
                assertThat(directOutput)
                        .contains("RPC_PROCESS_DIRECT_SUCCESS")
                        .contains("providerId=process-provider")
                        .contains("message=direct-process-call");
                assertThat(directInvocationId).isNotBlank();
                assertThat(processes.output(gateway))
                        .doesNotContain("invocationId=" + directInvocationId);

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
            int adminRpcPort,
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
                "--egon.cola.component.id.machine-id=1",
                "--egon.cola.component.ddc.enabled=false",
                "--egon.cola.component.ddc.admin.security.local-dev=true",
                "--egon.cola.component.ddc.admin.redis.host=" + redisHost,
                "--egon.cola.component.ddc.admin.redis.port=" + redisPort,
                "--egon.cola.component.rpc.enabled=true",
                "--egon.cola.component.rpc.provider.enabled=true",
                "--egon.cola.component.rpc.provider.port=" + adminRpcPort,
                "--egon.cola.component.rpc.provider.registration-mode=DISABLED",
                "--egon.cola.component.rpc.tls.development-plaintext=true",
                "--egon.cola.component.ddc.admin.rpc.signature-enabled=true",
                "--egon.cola.component.ddc.admin.rpc.credentials[0].credential-id=process-it",
                "--egon.cola.component.ddc.admin.rpc.credentials[0].access-key=process-it",
                "--egon.cola.component.ddc.admin.rpc.credentials[0].secret="
                        + "process-it-secret-at-least-32-bytes",
                "--egon.cola.component.ddc.admin.rpc.credentials[0].client-type=*",
                "--egon.cola.component.ddc.admin.rpc.credentials[0].app-code-patterns[0]=*",
                "--egon.cola.component.ddc.admin.rpc.credentials[0].env-patterns[0]=*",
                "--egon.cola.component.ddc.admin.rpc.credentials[0].biz-code-patterns[0]=*",
                "--egon.cola.component.ddc.admin.rpc.credentials[0].allowed-operations[0]=*"
        ));
        addPassword(
                arguments,
                "--egon.cola.component.ddc.admin.redis.password=",
                System.getenv("DDC_TEST_REDIS_PASSWORD")
        );
        return List.copyOf(arguments);
    }

    private List<String> rpcArguments(
            String adminTarget,
            String redisHost,
            int redisPort,
            String env,
            String namespace,
            List<String> roleArguments) {
        java.util.ArrayList<String> arguments = new java.util.ArrayList<>(
                List.of(
                        "--spring.main.web-application-type=none",
                        "--egon.cola.component.id.machine-id=2",
                        "--egon.cola.component.ddc.enabled=false",
                        "--egon.cola.component.ddc.registry.enabled=true",
                        "--egon.cola.component.ddc.rpc.target="
                                + adminTarget,
                        "--egon.cola.component.ddc.rpc.tls."
                                + "development-plaintext=true",
                        "--egon.cola.component.ddc.rpc.auth.registry.access-key=process-it",
                        "--egon.cola.component.ddc.rpc.auth.registry.secret-key="
                                + "process-it-secret-at-least-32-bytes",
                        "--egon.cola.component.ddc.redis.host=" + redisHost,
                        "--egon.cola.component.ddc.redis.port=" + redisPort,
                        "--egon.cola.component.ddc.biz-code=test-biz",
                        "--egon.cola.component.ddc.app-code=test-app",
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
            String adminTarget,
            String env,
            String namespace) {
        DdcProperties properties = new DdcProperties();
        properties.setBizCode("test-biz");
        properties.setAppCode("test-app");
        properties.setEnv(env);
        properties.setNamespace(namespace);
        DdcRpcProperties rpc = new DdcRpcProperties();
        rpc.setTarget(adminTarget);
        rpc.getTls().setDevelopmentPlaintext(true);
        rpc.getAuth().getRegistry().setAccessKey("process-it");
        rpc.getAuth().getRegistry().setSecretKey(
                "process-it-secret-at-least-32-bytes"
        );
        DdcRpcClientHandle<DdcServiceRegistryClient> handle =
                new DdcRpcClientFactory(
                        rpc,
                        properties,
                        new RpcProcessIdentity(
                                "rpc-process-it",
                                env,
                                "127.0.0.1",
                                ProcessHandle.current().pid(),
                                "rpc-process-it:" + ProcessHandle.current().pid()
                        )
                ).registryClient();
        return new RegistryResources(
                handle.client(),
                handle
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

    private void seedScope(
            Path database,
            String env,
            String namespace) throws Exception {
        try (var connection = DriverManager.getConnection(
                "jdbc:sqlite:" + database
        )) {
            connection.setAutoCommit(false);
            try (var statement = connection.createStatement()) {
                statement.addBatch(
                        "INSERT INTO ddc_biz (id, biz_code, biz_name, "
                                + "enabled, created_at, updated_at) VALUES "
                                + "('rpc-process-biz', 'test-biz', "
                                + "'RPC process test business', 1, "
                                + "strftime('%Y-%m-%d %H:%M:%f', 'now'), "
                                + "strftime('%Y-%m-%d %H:%M:%f', 'now'))"
                );
                statement.addBatch(
                        "INSERT INTO ddc_app (id, app_code, app_name, "
                                + "enabled, created_at, updated_at, biz_code) "
                                + "VALUES ('rpc-process-app', 'test-app', "
                                + "'RPC process test application', 1, "
                                + "strftime('%Y-%m-%d %H:%M:%f', 'now'), "
                                + "strftime('%Y-%m-%d %H:%M:%f', 'now'), "
                                + "'test-biz')"
                );
                statement.executeBatch();
            }
            try (var statement = connection.prepareStatement(
                    "INSERT INTO ddc_env (id, env_code, description, "
                            + "sort_order, enabled, created_at, updated_at) "
                            + "VALUES ('rpc-process-env', ?, "
                            + "'RPC process test environment', 0, 1, "
                            + "strftime('%Y-%m-%d %H:%M:%f', 'now'), "
                            + "strftime('%Y-%m-%d %H:%M:%f', 'now'))"
            )) {
                statement.setString(1, env);
                statement.executeUpdate();
            }
            try (var statement = connection.prepareStatement(
                    "INSERT INTO ddc_namespace (id, biz_code, "
                            + "namespace_code, namespace, enabled, "
                            + "created_at, updated_at) VALUES "
                            + "('rpc-process-namespace', 'test-biz', ?, ?, 1, "
                            + "strftime('%Y-%m-%d %H:%M:%f', 'now'), "
                            + "strftime('%Y-%m-%d %H:%M:%f', 'now'))"
            )) {
                statement.setString(1, namespace);
                statement.setString(2, namespace);
                statement.executeUpdate();
            }
            try (var statement = connection.prepareStatement(
                    "INSERT INTO ddc_namespace_env_app (id, namespace_id, "
                            + "env_code, app_id, enabled, created_at, "
                            + "updated_at) VALUES ('rpc-process-binding', "
                            + "'rpc-process-namespace', ?, "
                            + "'rpc-process-app', 1, "
                            + "strftime('%Y-%m-%d %H:%M:%f', 'now'), "
                            + "strftime('%Y-%m-%d %H:%M:%f', 'now'))"
            )) {
                statement.setString(1, env);
                statement.executeUpdate();
            }
            connection.commit();
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
            DdcRpcClientHandle<DdcServiceRegistryClient> handle
    ) implements AutoCloseable {

        @Override
        public void close() throws Exception {
            handle.close();
        }
    }
}
