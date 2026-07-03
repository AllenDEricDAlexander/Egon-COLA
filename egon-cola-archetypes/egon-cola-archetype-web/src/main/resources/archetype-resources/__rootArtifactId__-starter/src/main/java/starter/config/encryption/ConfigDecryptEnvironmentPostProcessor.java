package ${package}.starter.config.encryption;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.cloud.bootstrap.BootstrapConfigFileApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

public class ConfigDecryptEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private final ConfigDecryptor decryptor;
    private final ConfigDecryptKeyProvider keyProvider;

    public ConfigDecryptEnvironmentPostProcessor() {
        this(new AesGcmConfigDecryptor(), new ConfigDecryptKeyProvider());
    }

    ConfigDecryptEnvironmentPostProcessor(ConfigDecryptor decryptor, ConfigDecryptKeyProvider keyProvider) {
        this.decryptor = decryptor;
        this.keyProvider = keyProvider;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Optional<char[]> key = Optional.empty();
        boolean keyResolved = false;
        try {
            for (PropertySource<?> propertySource : environment.getPropertySources()) {
                if (!(propertySource instanceof EnumerablePropertySource<?> enumerablePropertySource)) {
                    continue;
                }
                Map<String, Object> decrypted = new LinkedHashMap<>();
                for (String propertyName : enumerablePropertySource.getPropertyNames()) {
                    Object value = enumerablePropertySource.getProperty(propertyName);
                    if (value instanceof CharSequence text && decryptor.supports(text.toString())) {
                        if (!keyResolved) {
                            key = keyProvider.resolveKey(environment);
                            keyResolved = true;
                        }
                        char[] resolvedKey = key.orElseThrow(() ->
                                new ConfigDecryptException("Encrypted configuration value requires EGON_CONFIG_DECRYPT_KEY"));
                        decrypted.put(propertyName, decryptor.decrypt(text.toString(), resolvedKey));
                    }
                }
                if (!decrypted.isEmpty()) {
                    environment.getPropertySources().addBefore(
                            propertySource.getName(),
                            new MapPropertySource(propertySource.getName() + "-decrypted", decrypted)
                    );
                }
            }
        } finally {
            key.ifPresent(value -> Arrays.fill(value, '\0'));
        }
    }

    @Override
    public int getOrder() {
        return BootstrapConfigFileApplicationListener.DEFAULT_ORDER + 1;
    }
}
