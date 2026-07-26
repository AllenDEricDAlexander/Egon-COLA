package top.egon.cola.component.gateway.test.deployment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.FileSystemResource;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayComposeConfigurationTest {

    private static final Path GATEWAY_DEPLOYMENT = Path.of(
            "egon-cola-components",
            "egon-cola-component-gateway",
            "deployment"
    );

    private static final Path HTTP_PROVIDER_APPLICATION = Path.of(
            "egon-cola-components",
            "egon-cola-component-gateway",
            "egon-cola-component-gateway-test",
            "egon-cola-component-gateway-test-http-provider",
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
        Properties environment = new Properties();
        try (InputStream input = Files.newInputStream(
                deploymentFile(".env.example")
        )) {
            environment.load(input);
        }

        assertThat(environment)
                .containsEntry("GATEWAY_RPC_SERVICE_NAME", "egon-gateway-rpc")
                .containsEntry("GATEWAY_RPC_GROUP", "default")
                .containsEntry("GATEWAY_RPC_VERSION", "1.0.0");
    }

    @Test
    void httpProviderReportingVersionFollowsServiceVersion()
            throws IOException {
        MutablePropertySources sources = new MutablePropertySources();
        sources.addFirst(new MapPropertySource(
                "test-override",
                Map.of("gateway.test.service-version", "2.0.0-test")
        ));
        new YamlPropertySourceLoader()
                .load(
                        "http-provider",
                        new FileSystemResource(projectFile(
                                HTTP_PROVIDER_APPLICATION
                        ))
                )
                .forEach(sources::addLast);
        PropertySourcesPropertyResolver resolver =
                new PropertySourcesPropertyResolver(sources);

        assertThat(resolver.getProperty(
                "egon.cola.component.gateway.reporting.artifact-version"
        )).isEqualTo("2.0.0-test");
    }

    private void assertEngineCoordinates(
            Map<String, Object> service,
            String advertisedHost) {
        Map<String, Object> environment = map(service.get("environment"));
        assertThat(environment)
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
    }

    private Map<String, Object> compose() throws IOException {
        try (InputStream input = Files.newInputStream(
                deploymentFile("compose.yml")
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
