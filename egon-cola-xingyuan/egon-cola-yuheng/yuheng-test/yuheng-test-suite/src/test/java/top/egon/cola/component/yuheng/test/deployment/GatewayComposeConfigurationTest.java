package top.egon.cola.component.yuheng.test.deployment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.FileSystemResource;
import org.yaml.snakeyaml.Yaml;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.yuheng.engine.common.config.GatewayEngineRuntimeProperties;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayComposeConfigurationTest {

    @Test
    void deploymentSeparatesFourRoleReplicasAndPreservesThePublicRoute() throws IOException {
        Map<String, Object> services = map(compose().get("services"));
        assertThat(services).containsKeys("yuheng-biz-gateway", "yuheng-biz-gateway-2",
                "yuheng-mcp-gateway", "yuheng-mcp-gateway-2", "yuheng-data-plane-proxy");
        var identities = new java.util.HashSet<Object>();
        var stateVolumes = new java.util.HashSet<String>();
        for (String name : List.of("yuheng-biz-gateway", "yuheng-biz-gateway-2", "yuheng-mcp-gateway", "yuheng-mcp-gateway-2")) {
            var service = map(services.get(name));
            var environment = map(service.get("environment"));
            boolean mcp = name.startsWith("yuheng-mcp");
            assertThat(environment).containsEntry("EGON_COLA_COMPONENT_TIANSHU_APP_CODE", mcp ? "gme" : "ge")
                    .containsEntry("EGON_COLA_COMPONENT_TIANSHU_BIZ_CODE", "${YUHENG_BIZ_CODE}")
                    .containsEntry("EGON_COLA_COMPONENT_TIANSHU_ENV", "${YUHENG_ENV:-local}")
                    .containsEntry("EGON_COLA_COMPONENT_TIANSHU_NAMESPACE", "${YUHENG_NAMESPACE:-default}");
            assertThat(identities.add(environment.get("EGON_COLA_COMPONENT_TIANSHU_INSTANCE_ID"))).isTrue();
            assertThat(stateVolumes.add(String.valueOf(list(service.get("volumes")).getFirst()))).isTrue();
            assertThat(map(service.get("healthcheck"))).containsKey("test");
            if (mcp) {
                assertThat(map(service.get("build")).get("context"))
                        .isEqualTo("../yuheng-mcp-gateway");
                assertThat(environment).containsEntry("YUHENG_MCP_POSTGRES_URL", "jdbc:postgresql://postgres:5432/gateway_admin")
                        .containsEntry("YUHENG_MCP_ARTIFACT_ROOT", "/var/lib/egon-yuheng-mcp/artifacts")
                        .containsEntry("EGON_COLA_COMPONENT_TIANSHU_RPC_AUTH_RUNTIME_ACCESS_KEY", "${YUHENG_MCP_TIANSHU_RUNTIME_ACCESS_KEY}");
                assertThat(environment.keySet()).noneMatch(key -> key.contains("YUHENG_ENGINE_RPC_"));
            } else {
                assertThat(environment.keySet()).noneMatch(key -> key.contains("MCP") || key.contains("DATASOURCE"));
                assertThat(environment).containsEntry("EGON_COLA_COMPONENT_TIANSHU_RPC_AUTH_RUNTIME_ACCESS_KEY", "${TIANSHU_RUNTIME_ACCESS_KEY}");
            }
        }
        var proxy = map(services.get("yuheng-data-plane-proxy"));
        assertThat(list(proxy.get("ports"))).contains("18081:18081");
        assertThat(list(map(services.get("yuheng-biz-gateway")).get("ports"))).doesNotContain("18081:18081");
        String routes = Files.readString(deploymentFile("haproxy.data-plane.cfg"));
        assertThat(routes).contains("path_beg /mcp/ /legacy/mcp/ /.well-known/oauth-protected-resource/mcp/",
                "use_backend gateway_mcp_engines if is_mcp", "default_backend gateway_api_rpc_engines",
                "yuheng-mcp-gateway:18084", "yuheng-mcp-gateway-2:18084", "/actuator/health/readiness");
        assertThat(Files.readString(deploymentFile("haproxy.cfg"))).doesNotContain("gateway_mcp_engines", "path_beg");
    }

    @Test
    void allOverlaysPreserveRoleIdentityStateTlsAndPortIsolation() throws IOException {
        Map<String, Object> base = map(compose().get("services"));
        var roles = List.of("yuheng-biz-gateway", "yuheng-biz-gateway-2", "yuheng-mcp-gateway", "yuheng-mcp-gateway-2");
        for (String file : List.of("compose.ha.yml", "compose.ha-mtls.yml")) {
            var services = map(compose(file).get("services"));
            for (String role : roles) {
                assertThat(map(map(services.get(role)).get("environment")))
                        .containsEntry("EGON_COLA_COMPONENT_TIANSHU_RPC_TARGET", "dns:///control-plane-proxy:19080");
            }
        }
        var tls = map(compose("compose.mtls.yml").get("services"));
        for (String role : roles) {
            var environment = map(map(tls.get(role)).get("environment"));
            assertThat(environment).containsEntry("EGON_COLA_COMPONENT_TIANSHU_RPC_TLS_CERTIFICATE_CHAIN_PATH",
                    "/run/egon-tls/" + role + ".crt");
            assertThat(environment).containsEntry("SERVER_ADDRESS", "0.0.0.0");
            if (role.startsWith("yuheng-mcp")) {
                assertThat(environment).containsEntry("YUHENG_MCP_ENGINE_CERTIFICATE_CHAIN_PATH", "/run/egon-tls/" + role + ".crt")
                        .containsEntry("YUHENG_MCP_ENGINE_DEVELOPMENT_PLAINTEXT", "false")
                        .containsEntry("YUHENG_MCP_ENGINE_OUTBOUND_RPC_DEVELOPMENT_PLAINTEXT", "false");
            } else {
                assertThat(environment).containsEntry("EGON_COLA_COMPONENT_YUHENG_ENGINE_TLS_RELOAD_ENABLED", "false");
            }
        }
        for (var services : List.of(base, tls)) {
            for (String first : List.of("yuheng-biz-gateway", "yuheng-mcp-gateway")) {
                assertThat(map(map(services.get(first)).get("environment")).keySet())
                        .containsExactlyInAnyOrderElementsOf(map(map(services.get(first + "-2")).get("environment")).keySet());
            }
        }
        var allServices = new LinkedHashMap<>(base);
        allServices.putAll(map(compose("compose.demo.yml").get("services")));
        var ports = new java.util.HashSet<String>();
        for (Object value : allServices.values()) {
            Object bindings = map(value).get("ports");
            if (bindings == null) {
                continue;
            }
            for (Object binding : list(bindings)) {
                String[] parts = binding.toString().split(":");
                assertThat(ports.add(parts[parts.length - 2])).as("unique host binding %s", binding).isTrue();
            }
        }
        String routes = Files.readString(deploymentFile("haproxy.data-plane.mtls.cfg"));
        assertThat(routes).contains("bind *:18081 ssl", "use_backend gateway_mcp_engines if is_mcp")
                .doesNotContain("verify none", "control-plane-proxy");
        for (String role : roles) {
            assertThat(routes).contains("verifyhost " + role + " ca-file /run/egon-tls/ca.crt");
        }
    }

    @Test
    void ddcReplicasLoadACompleteListWithSeparateMcpCredentials() throws IOException {
        Map<String, Object> config;
        try (InputStream input = Files.newInputStream(deploymentFile("tianshu-rpc-credentials.yml"))) {
            config = map(new Yaml().load(input));
        }
        for (String key : List.of("egon", "cola", "component", "tianshu", "admin", "rpc")) {
            config = map(config.get(key));
        }
        var credentials = list(config.get("credentials"));
        assertThat(credentials).hasSize(5);
        assertThat(credentials).extracting(value -> map(value).get("credential-id"))
                .containsExactly("runtime", "registry", "management", "yuheng-mcp-runtime", "yuheng-mcp-registry");
        assertThat(map(credentials.get(3))).containsEntry("access-key", "${YUHENG_MCP_TIANSHU_RUNTIME_ACCESS_KEY}");
        assertThat(map(credentials.get(4))).containsEntry("access-key", "${YUHENG_MCP_TIANSHU_REGISTRY_ACCESS_KEY}");
        for (var entry : Map.of("compose.yml", "tianshu-admin", "compose.ha.yml", "tianshu-admin-2").entrySet()) {
            var service = map(map(compose(entry.getKey()).get("services")).get(entry.getValue()));
            assertThat(map(service.get("environment")))
                    .containsEntry("SPRING_CONFIG_ADDITIONAL_LOCATION", "file:/run/egon-config/tianshu-rpc-credentials.yml")
                    .containsKeys("YUHENG_MCP_TIANSHU_RUNTIME_SECRET_KEY", "YUHENG_MCP_TIANSHU_REGISTRY_SECRET_KEY");
            assertThat(list(service.get("volumes"))).contains("./tianshu-rpc-credentials.yml:/run/egon-config/tianshu-rpc-credentials.yml:ro");
        }
    }

    private static final Path YUHENG_DEPLOYMENT = Path.of(
            "egon-cola-xingyuan",
            "egon-cola-yuheng",
            "deployment"
    );

    private static final Path HTTP_PROVIDER_APPLICATION = Path.of(
            "egon-cola-xingyuan",
            "egon-cola-yuheng",
            "yuheng-test",
            "yuheng-test-http-provider",
            "src",
            "main",
            "resources",
            "application.yml"
    );

    private static final Path WEBFLUX_PROVIDER_APPLICATION = Path.of(
            "egon-cola-xingyuan",
            "egon-cola-yuheng",
            "yuheng-test",
            "yuheng-test-webflux-http-provider",
            "src",
            "main",
            "resources",
            "application.yml"
    );

    @Test
    void enginesUseComposeResolvableDdcAndRpcCoordinates() throws IOException {
        Map<String, Object> compose = compose();
        Map<String, Object> services = map(compose.get("services"));

        assertEngineCoordinates(
                map(services.get("yuheng-biz-gateway")),
                "yuheng-biz-gateway"
        );
        assertEngineCoordinates(
                map(services.get("yuheng-biz-gateway-2")),
                "yuheng-biz-gateway-2"
        );
        assertThat(map(map(services.get("yuheng-admin")).get("environment")))
                .doesNotContainKeys(
                        "EGON_COLA_COMPONENT_TIANSHU_REDIS_HOST",
                        "EGON_COLA_COMPONENT_TIANSHU_REDIS_PORT"
                );
    }

    @Test
    void ddcAdminExposesDirectRpcAlongsideHumanHttp() throws IOException {
        Map<String, Object> service = map(
                map(compose().get("services")).get("tianshu-admin")
        );
        Map<String, Object> environment = map(service.get("environment"));

        assertThat(environment)
                .containsEntry("TIANSHU_RPC_PORT", 19080)
                .containsEntry(
                        "TIANSHU_RPC_DEVELOPMENT_PLAINTEXT",
                        "true"
                )
                .containsKeys(
                        "TIANSHU_RPC_RUNTIME_ACCESS_KEY",
                        "TIANSHU_RPC_RUNTIME_SECRET_KEY",
                        "TIANSHU_RPC_REGISTRY_ACCESS_KEY",
                        "TIANSHU_RPC_REGISTRY_SECRET_KEY",
                        "TIANSHU_RPC_MANAGEMENT_ACCESS_KEY",
                        "TIANSHU_RPC_MANAGEMENT_SECRET_KEY"
                );
        assertThat(list(service.get("ports")))
                .contains("18070:18080", "19080:19080");
        assertThat(map(service.get("healthcheck")))
                .containsKey("test");
        assertNoLegacyDdcHttpConfiguration(environment);
    }

    @Test
    void consumersUseDirectDdcRpcWithoutDiscoveryBootstrap()
            throws IOException {
        Map<String, Object> services = map(compose().get("services"));
        for (String serviceName : List.of(
                "yuheng-admin",
                "yuheng-biz-gateway",
                "yuheng-biz-gateway-2")) {
            Map<String, Object> environment = map(
                    map(services.get(serviceName)).get("environment")
            );
            assertThat(environment)
                    .as(serviceName)
                    .containsEntry(
                            "EGON_COLA_COMPONENT_TIANSHU_RPC_TARGET",
                            "dns:///tianshu-admin:19080"
                    )
                    .containsEntry(
                            "EGON_COLA_COMPONENT_TIANSHU_RPC_LOAD_BALANCING_POLICY",
                            "round_robin"
                    );
            assertNoLegacyDdcHttpConfiguration(environment);
        }
        assertThat(map(map(services.get("yuheng-admin"))
                .get("environment")))
                .containsKeys(
                        "EGON_COLA_COMPONENT_TIANSHU_RPC_AUTH_"
                                + "MANAGEMENT_ACCESS_KEY",
                        "EGON_COLA_COMPONENT_TIANSHU_RPC_AUTH_"
                                + "MANAGEMENT_SECRET_KEY"
                );
        for (String serviceName : List.of(
                "yuheng-biz-gateway",
                "yuheng-biz-gateway-2")) {
            assertThat(map(map(services.get(serviceName))
                    .get("environment")))
                    .as(serviceName)
                    .containsKeys(
                            "EGON_COLA_COMPONENT_TIANSHU_RPC_AUTH_"
                                    + "RUNTIME_ACCESS_KEY",
                            "EGON_COLA_COMPONENT_TIANSHU_RPC_AUTH_"
                                    + "REGISTRY_ACCESS_KEY"
                    );
        }
    }

    @Test
    void demoApplicationsUseDirectDdcRpcProfiles()
            throws IOException {
        Map<String, Object> services = map(
                compose("compose.demo.yml").get("services")
        );
        for (String serviceName : List.of(
                "http-provider-mvc",
                "http-provider-webflux",
                "rpc-provider",
                "rpc-consumer")) {
            Map<String, Object> environment = map(
                    map(services.get(serviceName)).get("environment")
            );
            assertThat(environment)
                    .as(serviceName)
                    .containsEntry(
                            "EGON_COLA_COMPONENT_TIANSHU_RPC_TARGET",
                            "dns:///tianshu-admin:19080"
                    )
                    .containsKeys(
                            "EGON_COLA_COMPONENT_TIANSHU_RPC_AUTH_"
                                    + "RUNTIME_ACCESS_KEY",
                            "EGON_COLA_COMPONENT_TIANSHU_RPC_AUTH_"
                                    + "REGISTRY_ACCESS_KEY"
                    );
            assertNoLegacyDdcHttpConfiguration(environment);
        }
    }

    @Test
    void haComposeBalancesDdcRpcAcrossActiveActiveNodes()
            throws IOException {
        Map<String, Object> services = map(
                compose("compose.ha.yml").get("services")
        );
        Map<String, Object> secondDdc = map(services.get("tianshu-admin-2"));
        Map<String, Object> secondDdcEnvironment = map(
                secondDdc.get("environment")
        );
        Map<String, Object> proxy = map(
                services.get("control-plane-proxy")
        );

        assertThat(secondDdcEnvironment)
                .containsEntry("TIANSHU_RPC_PORT", 19080)
                .containsEntry(
                        "SPRING_DATASOURCE_URL",
                        "jdbc:postgresql://postgres:5432/gateway_ddc"
                )
                .containsEntry(
                        "EGON_COLA_COMPONENT_TIANSHU_ADMIN_REDIS_HOST",
                        "tianshu-redis"
                );
        assertThat(list(secondDdc.get("ports")))
                .contains("18170:18080", "19180:19080");
        assertThat(list(proxy.get("ports"))).contains("19280:19080");

        for (String serviceName : List.of(
                "yuheng-admin",
                "yuheng-admin-2",
                "yuheng-biz-gateway",
                "yuheng-biz-gateway-2")) {
            Map<String, Object> environment = map(
                    map(services.get(serviceName)).get("environment")
            );
            assertThat(environment)
                    .as(serviceName)
                    .containsEntry(
                            "EGON_COLA_COMPONENT_TIANSHU_RPC_TARGET",
                            "dns:///control-plane-proxy:19080"
                    );
            assertNoLegacyDdcHttpConfiguration(environment);
        }
    }

    @Test
    void envProvidesStableRpcServiceIdentityDefaults() throws IOException {
        Properties environment = deploymentEnvironment();

        assertThat(environment)
                .containsEntry("YUHENG_BIZ_CODE", "default")
                .containsEntry("YUHENG_RPC_SERVICE_NAME", "egon-yuheng-rpc")
                .containsEntry("YUHENG_RPC_GROUP", "default")
                .containsEntry("YUHENG_RPC_VERSION", "1.0.0");
    }

    @Test
    void servicesUseDistinctExplicitMachineIds() throws IOException {
        Map<String, Object> services = map(compose().get("services"));
        assertMachineId(services, "tianshu-admin", "1");
        assertMachineId(services, "yuheng-admin", "2");
        assertMachineId(services, "yuheng-biz-gateway", "10");
        assertMachineId(services, "yuheng-biz-gateway-2", "11");

        Map<String, Object> haServices = map(
                compose("compose.ha.yml").get("services")
        );
        assertMachineId(haServices, "tianshu-admin-2", "3");
        assertMachineId(haServices, "yuheng-admin-2", "4");
    }

    @Test
    void httpProviderReportingVersionFollowsServiceVersion()
            throws IOException {
        assertReportingVersionFollowsServiceVersion(
                HTTP_PROVIDER_APPLICATION
        );
        assertReportingVersionFollowsServiceVersion(
                WEBFLUX_PROVIDER_APPLICATION
        );
    }

    @Test
    void composeBuildInputsResolveAfterPlatformMigration()
            throws IOException {
        for (String fileName : List.of(
                "compose.yml",
                "compose.ha.yml",
                "compose.demo.yml")) {
            assertBuildInputsResolve(fileName);
        }
    }

    @Test
    void demoComposeUsesExecutableTestApplicationArtifacts()
            throws IOException {
        Map<String, Object> services = map(
                compose("compose.demo.yml").get("services")
        );
        Map<String, String> expectedArtifacts = Map.of(
                "http-provider-mvc",
                "yuheng-test-http-provider-exec.jar",
                "http-provider-webflux",
                "yuheng-test-webflux-http-provider-exec.jar",
                "rpc-provider",
                "yuheng-test-rpc-provider-exec.jar",
                "rpc-consumer",
                "yuheng-test-rpc-consumer-exec.jar"
        );

        expectedArtifacts.forEach((serviceName, artifactName) -> {
            Map<String, Object> service = map(services.get(serviceName));
            Map<String, Object> build = map(service.get("build"));
            Map<String, Object> arguments = map(build.get("args"));

            assertThat(arguments.get("APP_JAR"))
                    .as(serviceName)
                    .isEqualTo(testApplicationArtifact(
                            serviceName,
                            artifactName
                    ));
        });
    }

    private void assertReportingVersionFollowsServiceVersion(
            Path application) throws IOException {
        MutablePropertySources sources = new MutablePropertySources();
        sources.addFirst(new MapPropertySource(
                "test-override",
                Map.of("yuheng.test.service-version", "2.0.0-test")
        ));
        new YamlPropertySourceLoader()
                .load(
                        "http-provider",
                        new FileSystemResource(projectFile(
                                application
                        ))
                )
                .forEach(sources::addLast);
        PropertySourcesPropertyResolver resolver =
                new PropertySourcesPropertyResolver(sources);

        assertThat(resolver.getProperty(
                "egon.cola.component.yuheng.openapi.artifact-version"
        )).isEqualTo("2.0.0-test");
    }

    private void assertMachineId(
            Map<String, Object> services,
            String serviceName,
            String expectedMachineId) {
        Map<String, Object> environment = map(
                map(services.get(serviceName)).get("environment")
        );
        assertThat(environment).containsEntry(
                "EGON_COLA_COMPONENT_ID_MACHINE_ID",
                expectedMachineId
        );
    }

    private void assertEngineCoordinates(
            Map<String, Object> service,
            String advertisedHost) throws IOException {
        Map<String, Object> environment = map(service.get("environment"));
        assertThat(environment)
                .containsEntry(
                        "EGON_COLA_COMPONENT_TIANSHU_BIZ_CODE",
                        "${YUHENG_BIZ_CODE}"
                )
                .containsEntry("EGON_COLA_COMPONENT_TIANSHU_REDIS_HOST", "tianshu-redis")
                .containsEntry("EGON_COLA_COMPONENT_TIANSHU_REDIS_PORT", 6379)
                .containsEntry(
                        "EGON_COLA_COMPONENT_YUHENG_ENGINE_RPC_ADVERTISED_HOST",
                        advertisedHost
                )
                .containsEntry(
                        "EGON_COLA_COMPONENT_YUHENG_ENGINE_RPC_SERVICE_NAME",
                        "${YUHENG_RPC_SERVICE_NAME}"
                )
                .containsEntry(
                        "EGON_COLA_COMPONENT_YUHENG_ENGINE_RPC_GROUP",
                        "${YUHENG_RPC_GROUP}"
                )
                .containsEntry(
                        "EGON_COLA_COMPONENT_YUHENG_ENGINE_RPC_VERSION",
                        "${YUHENG_RPC_VERSION}"
                );

        StandardEnvironment springEnvironment = new StandardEnvironment();
        springEnvironment.getPropertySources().replace(
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                new SystemEnvironmentPropertySource(
                        StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                        resolvedEnvironment(environment)
                )
        );
        ConfigurationPropertySources.attach(springEnvironment);
        DdcProperties ddc = Binder.get(springEnvironment)
                .bind(
                        "egon.cola.component.tianshu",
                        DdcProperties.class
                )
                .orElseThrow(() -> new IllegalStateException(
                        "Tianshu Compose environment did not bind"
                ));
        GatewayEngineRuntimeProperties engine = Binder.get(springEnvironment)
                .bind(
                        "egon.cola.component.yuheng.engine",
                        GatewayEngineRuntimeProperties.class
                )
                .orElseThrow(() -> new IllegalStateException(
                        "Gateway Engine Compose environment did not bind"
                ));

        assertThat(ddc.getBizCode()).isEqualTo("default");
        assertThat(ddc.getRedis().getHost()).isEqualTo("tianshu-redis");
        assertThat(ddc.getRedis().getPort()).isEqualTo(6379);
        assertThat(engine.getRpc().getAdvertisedHost())
                .isEqualTo(advertisedHost);
        assertThat(engine.getRpc().getServiceName())
                .isEqualTo("egon-yuheng-rpc");
        assertThat(engine.getRpc().getGroup()).isEqualTo("default");
        assertThat(engine.getRpc().getVersion()).isEqualTo("1.0.0");
    }

    private void assertNoLegacyDdcHttpConfiguration(
            Map<String, Object> environment) {
        assertThat(environment.keySet())
                .noneMatch(key -> key.contains("TIANSHU_ADMIN_ENDPOINT"))
                .noneMatch(key -> key.contains("TIANSHU_ADMIN_OPENAPI"))
                .noneMatch(key -> key.startsWith("TIANSHU_OPENAPI_"))
                .noneMatch(key -> key.startsWith("YUHENG_ADMIN_TIANSHU_ENDPOINT"));
    }

    private Map<String, Object> resolvedEnvironment(
            Map<String, Object> environment) throws IOException {
        Properties defaults = deploymentEnvironment();
        Map<String, Object> resolved = new LinkedHashMap<>();
        environment.forEach((key, value) -> resolved.put(
                key,
                resolve(value, defaults)
        ));
        return resolved;
    }

    private Object resolve(Object value, Properties defaults) {
        if (!(value instanceof String text)
                || !text.startsWith("${")
                || !text.endsWith("}")) {
            return value;
        }
        String key = text.substring(2, text.length() - 1);
        return defaults.getProperty(key, text);
    }

    private Properties deploymentEnvironment() throws IOException {
        Properties environment = new Properties();
        try (InputStream input = Files.newInputStream(
                deploymentFile(".env.example")
        )) {
            environment.load(input);
        }
        return environment;
    }

    private void assertBuildInputsResolve(String fileName)
            throws IOException {
        Path composeFile = deploymentFile(fileName);
        Path deploymentDirectory = composeFile.getParent();
        Map<String, Object> services = map(
                compose(fileName).get("services")
        );

        services.forEach((serviceName, configuration) -> {
            Map<String, Object> service = map(configuration);
            if (!(service.get("build") instanceof Map<?, ?>)) {
                return;
            }
            Map<String, Object> build = map(service.get("build"));
            Path context = deploymentDirectory
                    .resolve(String.valueOf(build.get("context")))
                    .normalize();
            assertThat(context)
                    .as(fileName + ":" + serviceName + " build context")
                    .isDirectory();

            if (build.containsKey("dockerfile")) {
                Path dockerfile = context
                        .resolve(String.valueOf(build.get("dockerfile")))
                        .normalize();
                assertThat(dockerfile)
                        .as(fileName + ":" + serviceName + " dockerfile")
                        .isRegularFile();
            }
        });
    }

    private String testApplicationArtifact(
            String serviceName,
            String artifactName) {
        String moduleName = switch (serviceName) {
            case "http-provider-mvc" -> "http-provider";
            case "http-provider-webflux" -> "webflux-http-provider";
            case "rpc-provider" -> "rpc-provider";
            case "rpc-consumer" -> "rpc-consumer";
            default -> throw new IllegalArgumentException(
                    "unknown demo service " + serviceName
            );
        };
        return "egon-cola-xingyuan/egon-cola-yuheng/"
                + "yuheng-test/"
                + "yuheng-test-" + moduleName
                + "/target/" + artifactName;
    }

    private Map<String, Object> compose() throws IOException {
        return compose("compose.yml");
    }

    private Map<String, Object> compose(String fileName) throws IOException {
        try (InputStream input = Files.newInputStream(
                deploymentFile(fileName)
        )) {
            return map(new Yaml().load(input));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Object> list(Object value) {
        return (List<Object>) value;
    }

    private Path deploymentFile(String fileName) {
        return projectFile(YUHENG_DEPLOYMENT.resolve(fileName));
    }

    private Path projectFile(Path projectPath) {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(projectPath);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException(
                "cannot locate project file " + projectPath
        );
    }
}
