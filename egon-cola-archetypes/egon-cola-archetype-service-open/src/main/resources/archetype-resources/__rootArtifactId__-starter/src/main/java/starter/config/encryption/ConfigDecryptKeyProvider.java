package ${package}.starter.config.encryption;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

public class ConfigDecryptKeyProvider {

    private static final String KEY_ENV = "EGON_CONFIG_DECRYPT_KEY";
    private static final String KEY_FILE_ENV = "EGON_CONFIG_DECRYPT_KEY_FILE";
    private static final Path DEFAULT_KEY_FILE = Path.of("/run/secrets/egon_config_decrypt_key");
    private final Path defaultKeyFile;

    public ConfigDecryptKeyProvider() {
        this(DEFAULT_KEY_FILE);
    }

    ConfigDecryptKeyProvider(Path defaultKeyFile) {
        this.defaultKeyFile = defaultKeyFile;
    }

    public Optional<char[]> resolveKey() {
        return resolveKey(System.getenv(KEY_ENV), System.getenv(KEY_FILE_ENV), true);
    }

    public Optional<char[]> resolveKey(ConfigurableEnvironment environment) {
        if (environment == null) {
            return resolveKey();
        }
        PropertySource<?> systemEnvironment = environment.getPropertySources()
                .get(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        if (systemEnvironment == null) {
            return resolveKey();
        }
        return resolveKey(
                systemEnvironment.getProperty(KEY_ENV),
                systemEnvironment.getProperty(KEY_FILE_ENV),
                true
        );
    }

    private Optional<char[]> resolveKey(Object keyValue, Object keyFileValue, boolean includeDefaultFile) {
        if (keyValue instanceof String key && !key.isBlank()) {
            return Optional.of(key.toCharArray());
        }
        if (keyFileValue instanceof String keyFile && !keyFile.isBlank()) {
            return Optional.of(readKey(Path.of(keyFile)));
        }
        if (includeDefaultFile && Files.isRegularFile(defaultKeyFile)) {
            return Optional.of(readKey(defaultKeyFile));
        }
        return Optional.empty();
    }

    private char[] readKey(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8).trim().toCharArray();
        } catch (IOException ex) {
            throw new ConfigDecryptException("Failed to read configuration decrypt key file", ex);
        }
    }
}
