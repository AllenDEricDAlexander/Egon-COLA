package top.egon.cola.component.gateway.test.deployment;

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
import top.egon.cola.component.ddc.config.DdcProperties;
import top.egon.cola.component.gateway.engine.GatewayEngineRuntimeProperties;

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

    private static final Path GATEWAY_DEPLOYMENT = Path.of(
            "egon-cola-platforms",
            "egon-cola-platform-gateway",
            "deployment"
    );

    private static final Path HTTP_PROVIDER_APPLICATION = Path.of(
            "egon-cola-platforms",
            "egon-cola-platform-gateway",
            "egon-cola-platform-gateway-test",
            "egon-cola-platform-gateway-test-http-provider",
            "src",
            "main",
            "resources",
            "application.yml"
    );

    private static final Path WEBFLUX_PROVIDER_APPLICATION = Path.of(
            "egon-cola-platforms",
            "egon-cola-platform-gateway",
            "egon-cola-platform-gateway-test",
            "egon-cola-platform-gateway-test-webflux-http-provider",
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
                map(services.get("gateway-engine")),
                "gateway-engine"
        );
        assertEngineCoordinates(
                map(services.get("gateway-engine-2")),
                "gateway-engine-2"
        );
        assertThat(map(map(services.get("gateway-admin")).get("environment")))
                .doesNotContainKeys(
                        "EGON_COLA_COMPONENT_DDC_REDIS_HOST",
                        "EGON_COLA_COMPONENT_DDC_REDIS_PORT"
                );
    }

    @Test
    void envProvidesStableRpcServiceIdentityDefaults() throws IOException {
        Properties environment = deploymentEnvironment();

        assertThat(environment)
                .containsEntry("GATEWAY_BIZ_CODE", "default")
                .containsEntry("GATEWAY_RPC_SERVICE_NAME", "egon-gateway-rpc")
                .containsEntry("GATEWAY_RPC_GROUP", "default")
                .containsEntry("GATEWAY_RPC_VERSION", "1.0.0");
    }

    @Test
    void servicesUseDistinctExplicitMachineIds() throws IOException {
        Map<String, Object> services = map(compose().get("services"));
        assertMachineId(services, "ddc-admin", "1");
        assertMachineId(services, "gateway-admin", "2");
        assertMachineId(services, "gateway-engine", "10");
        assertMachineId(services, "gateway-engine-2", "11");

        Map<String, Object> haServices = map(
                compose("compose.ha.yml").get("services")
        );
        assertMachineId(haServices, "ddc-admin-2", "3");
        assertMachineId(haServices, "gateway-admin-2", "4");
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
                "gateway-test-http-provider-exec.jar",
                "http-provider-webflux",
                "gateway-test-webflux-http-provider-exec.jar",
                "rpc-provider",
                "gateway-test-rpc-provider-exec.jar",
                "rpc-consumer",
                "gateway-test-rpc-consumer-exec.jar"
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
                Map.of("gateway.test.service-version", "2.0.0-test")
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
                "egon.cola.component.gateway.reporting.artifact-version"
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
                        "EGON_COLA_COMPONENT_DDC_BIZ_CODE",
                        "${GATEWAY_BIZ_CODE}"
                )
                .containsEntry("EGON_COLA_COMPONENT_DDC_REDIS_HOST", "ddc-redis")
                .containsEntry("EGON_COLA_COMPONENT_DDC_REDIS_PORT", 6379)
                .containsEntry(
                        "EGON_COLA_COMPONENT_GATEWAY_ENGINE_RPC_ADVERTISED_HOST",
                        advertisedHost
                )
                .containsEntry(
                        "EGON_COLA_COMPONENT_GATEWAY_ENGINE_RPC_SERVICE_NAME",
                        "${GATEWAY_RPC_SERVICE_NAME}"
                )
                .containsEntry(
                        "EGON_COLA_COMPONENT_GATEWAY_ENGINE_RPC_GROUP",
                        "${GATEWAY_RPC_GROUP}"
                )
                .containsEntry(
                        "EGON_COLA_COMPONENT_GATEWAY_ENGINE_RPC_VERSION",
                        "${GATEWAY_RPC_VERSION}"
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
                        "egon.cola.component.ddc",
                        DdcProperties.class
                )
                .orElseThrow(() -> new IllegalStateException(
                        "DDC Compose environment did not bind"
                ));
        GatewayEngineRuntimeProperties engine = Binder.get(springEnvironment)
                .bind(
                        "egon.cola.component.gateway.engine",
                        GatewayEngineRuntimeProperties.class
                )
                .orElseThrow(() -> new IllegalStateException(
                        "Gateway Engine Compose environment did not bind"
                ));

        assertThat(ddc.getBizCode()).isEqualTo("default");
        assertThat(ddc.getRedis().getHost()).isEqualTo("ddc-redis");
        assertThat(ddc.getRedis().getPort()).isEqualTo(6379);
        assertThat(engine.getRpc().getAdvertisedHost())
                .isEqualTo(advertisedHost);
        assertThat(engine.getRpc().getServiceName())
                .isEqualTo("egon-gateway-rpc");
        assertThat(engine.getRpc().getGroup()).isEqualTo("default");
        assertThat(engine.getRpc().getVersion()).isEqualTo("1.0.0");
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
        return "egon-cola-platforms/egon-cola-platform-gateway/"
                + "egon-cola-platform-gateway-test/"
                + "egon-cola-platform-gateway-test-" + moduleName
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

    private Path deploymentFile(String fileName) {
        return projectFile(GATEWAY_DEPLOYMENT.resolve(fileName));
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
