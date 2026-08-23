package ${package}.infrastructure.config.datasource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Loads a ShardingSphere rule resource and resolves only Spring placeholders.
 */
public final class ShardingYamlLoader {

    private static final String SHARDING_RULE_MARKER = "  - !SHARDING";

    private final ResourceLoader resourceLoader;
    private final Environment environment;

    public ShardingYamlLoader(ResourceLoader resourceLoader, Environment environment) {
        this.resourceLoader = resourceLoader;
        this.environment = environment;
    }

    public byte[] load(String location) {
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("ShardingSphere rule location must not be blank");
        }
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new IllegalArgumentException(
                    "ShardingSphere rule resource does not exist: " + location);
        }
        try (var inputStream = resource.getInputStream()) {
            String yaml = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return environment.resolveRequiredPlaceholders(yaml)
                    .getBytes(StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalArgumentException(
                    "Unable to read ShardingSphere rule resource: " + location, failure);
        }
    }

    public byte[] shardingRuleSuffix(byte[] yaml) {
        if (yaml == null) {
            throw new IllegalArgumentException("ShardingSphere rule content must not be null");
        }
        String content = new String(yaml, StandardCharsets.UTF_8);
        int marker = content.indexOf(SHARDING_RULE_MARKER);
        if (marker < 0) {
            throw new IllegalArgumentException("ShardingSphere rule must contain !SHARDING");
        }
        return content.substring(marker).getBytes(StandardCharsets.UTF_8);
    }
}
