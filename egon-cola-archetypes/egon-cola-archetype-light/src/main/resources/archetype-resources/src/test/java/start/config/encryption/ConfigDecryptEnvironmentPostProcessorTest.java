package ${package}.start.config.encryption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.cloud.bootstrap.BootstrapConfigFileApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class ConfigDecryptEnvironmentPostProcessorTest {

    @Test
    void runsAfterSpringCloudBootstrapConfigFileApplicationListener() {
        assertThat(new ConfigDecryptEnvironmentPostProcessor().getOrder())
                .isEqualTo(BootstrapConfigFileApplicationListener.DEFAULT_ORDER + 1);
    }

    @Test
    void decryptsConfigDataLoadedThroughSpringFactories(@TempDir Path tempDir) throws Exception {
        String keyText = "12345678901234567890123456789012";
        char[] key = keyText.toCharArray();
        try {
            String encrypted = ConfigCipherCli.encrypt(key, "from-config-data");
            Path properties = tempDir.resolve("application.properties");
            Files.writeString(properties, "secret.value=" + encrypted + System.lineSeparator());
            StandardEnvironment environment = new StandardEnvironment();
            environment.getPropertySources().replace(
                    StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                    new MapPropertySource(
                            StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                            Map.of("EGON_CONFIG_DECRYPT_KEY", keyText)
                    )
            );
            SpringApplication application = new SpringApplication(TestApplication.class);
            application.setWebApplicationType(WebApplicationType.NONE);
            application.setEnvironment(environment);
            application.setDefaultProperties(Map.of("spring.config.location", properties.toUri().toString()));

            try (ConfigurableApplicationContext context = application.run()) {
                assertThat(context.getEnvironment().getProperty("secret.value")).isEqualTo("from-config-data");
            }
        } finally {
            Arrays.fill(key, '\0');
        }
    }

    @Test
    void decryptsBootstrapConfigLoadedThroughSpringFactories(@TempDir Path tempDir) throws Exception {
        String keyText = "12345678901234567890123456789012";
        char[] key = keyText.toCharArray();
        try {
            String encrypted = ConfigCipherCli.encrypt(key, "from-bootstrap");
            Path bootstrap = tempDir.resolve("bootstrap.properties");
            Files.writeString(bootstrap, "secret.value=" + encrypted + System.lineSeparator());
            StandardEnvironment environment = new StandardEnvironment();
            environment.getPropertySources().replace(
                    StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                    new MapPropertySource(
                            StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                            Map.of("EGON_CONFIG_DECRYPT_KEY", keyText)
                    )
            );
            SpringApplication application = new SpringApplication(TestApplication.class);
            application.setWebApplicationType(WebApplicationType.NONE);
            application.setEnvironment(environment);
            application.setDefaultProperties(Map.of(
                    "spring.cloud.bootstrap.additional-location",
                    tempDir.toUri().toString()
            ));

            try (ConfigurableApplicationContext context = application.run()) {
                assertThat(context.getEnvironment().getProperty("secret.value")).isEqualTo("from-bootstrap");
            }
        } finally {
            Arrays.fill(key, '\0');
        }
    }

    @Test
    void decryptsConfigtreeValuesLoadedThroughSpringFactories(@TempDir Path tempDir) throws Exception {
        String keyText = "12345678901234567890123456789012";
        char[] key = keyText.toCharArray();
        try {
            String encrypted = ConfigCipherCli.encrypt(key, "from-configtree");
            Path configtree = tempDir.resolve("configtree");
            Files.createDirectory(configtree);
            Files.writeString(configtree.resolve("secret.value"), encrypted);
            StandardEnvironment environment = new StandardEnvironment();
            environment.getPropertySources().replace(
                    StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                    new MapPropertySource(
                            StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                            Map.of("EGON_CONFIG_DECRYPT_KEY", keyText)
                    )
            );
            SpringApplication application = new SpringApplication(TestApplication.class);
            application.setWebApplicationType(WebApplicationType.NONE);
            application.setEnvironment(environment);
            application.setDefaultProperties(Map.of(
                    "spring.config.import",
                    "optional:configtree:" + configtree.toAbsolutePath().toString().replace('\\', '/') + "/"
            ));

            try (ConfigurableApplicationContext context = application.run()) {
                assertThat(context.getEnvironment().getProperty("secret.value")).isEqualTo("from-configtree");
            }
        } finally {
            Arrays.fill(key, '\0');
        }
    }

    @Test
    void decryptsCharSequenceValuesAndLeavesNonTextValuesUntouched() throws Exception {
        char[] key = "12345678901234567890123456789012".toCharArray();
        String encrypted = ConfigCipherCli.encrypt(key, "from-char-sequence");
        Integer plainNumber = 42;
        TrackingKeyProvider keyProvider = new TrackingKeyProvider(Optional.of(key));
        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "test",
                Map.of(
                        "secret.value", new StringBuilder(encrypted),
                        "plain.number", plainNumber
                )
        ));

        new ConfigDecryptEnvironmentPostProcessor(new AesGcmConfigDecryptor(), keyProvider)
                .postProcessEnvironment(environment, null);

        assertThat(keyProvider.resolveCount()).isOne();
        assertThat(environment.getProperty("secret.value")).isEqualTo("from-char-sequence");
        assertThat(environment.getPropertySources().get("test").getProperty("plain.number")).isSameAs(plainNumber);
    }

    @Test
    void ignoresMissingKeyWhenNoEncryptedValues() {
        TrackingKeyProvider keyProvider = new TrackingKeyProvider(Optional.empty());
        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "test",
                Map.of("plain.value", "plain-text")
        ));

        new ConfigDecryptEnvironmentPostProcessor(new AesGcmConfigDecryptor(), keyProvider)
                .postProcessEnvironment(environment, null);

        assertThat(keyProvider.resolveCount()).isZero();
        assertThat(environment.getProperty("plain.value")).isEqualTo("plain-text");
    }

    @Test
    void resolvesKeyOnceForEncryptedValues() throws Exception {
        char[] key = "12345678901234567890123456789012".toCharArray();
        String first = ConfigCipherCli.encrypt(key, "first-secret");
        String second = ConfigCipherCli.encrypt(key, "second-secret");
        TrackingKeyProvider keyProvider = new TrackingKeyProvider(Optional.of(key));
        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "test",
                Map.of(
                        "first.secret", first,
                        "second.secret", second
                )
        ));

        new ConfigDecryptEnvironmentPostProcessor(new AesGcmConfigDecryptor(), keyProvider)
                .postProcessEnvironment(environment, null);

        assertThat(keyProvider.resolveCount()).isOne();
        assertThat(environment.getProperty("first.secret")).isEqualTo("first-secret");
        assertThat(environment.getProperty("second.secret")).isEqualTo("second-secret");
    }

    @Test
    void rejectsEncryptedValueWhenKeyMissing() throws Exception {
        char[] key = "12345678901234567890123456789012".toCharArray();
        String encrypted = ConfigCipherCli.encrypt(key, "secret-value");
        TrackingKeyProvider keyProvider = new TrackingKeyProvider(Optional.empty());
        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "test",
                Map.of("secret.value", encrypted)
        ));

        assertThatThrownBy(() -> new ConfigDecryptEnvironmentPostProcessor(new AesGcmConfigDecryptor(), keyProvider)
                .postProcessEnvironment(environment, null))
                .isInstanceOf(ConfigDecryptException.class)
                .hasMessage("Encrypted configuration value requires EGON_CONFIG_DECRYPT_KEY");
        assertThat(keyProvider.resolveCount()).isOne();
    }

    @Test
    void fallsBackToDefaultKeyFileAfterSpringSystemEnvironment(@TempDir Path tempDir) throws Exception {
        String keyText = "12345678901234567890123456789012";
        char[] key = keyText.toCharArray();
        try {
            String encrypted = ConfigCipherCli.encrypt(key, "secret-value");
            Path defaultKeyFile = tempDir.resolve("egon_config_decrypt_key");
            Files.writeString(defaultKeyFile, keyText);
            ConfigurableEnvironment environment = new StandardEnvironment();
            environment.getPropertySources().replace(
                    StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                    new MapPropertySource(
                            StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                            Map.of()
                    )
            );
            environment.getPropertySources().addFirst(new MapPropertySource(
                    "test",
                    Map.of("secret.value", encrypted)
            ));

            new ConfigDecryptEnvironmentPostProcessor(
                    new AesGcmConfigDecryptor(),
                    new ConfigDecryptKeyProvider(defaultKeyFile)
            ).postProcessEnvironment(environment, null);

            assertThat(environment.getProperty("secret.value")).isEqualTo("secret-value");
        } finally {
            Arrays.fill(key, '\0');
        }
    }

    @Test
    void doesNotFallBackToHostEnvironmentWhenSpringEnvironmentKeyIsMissing(@TempDir Path tempDir) throws Exception {
        char[] key = "12345678901234567890123456789012".toCharArray();
        try {
            String encrypted = ConfigCipherCli.encrypt(key, "secret-value");
            ConfigurableEnvironment environment = new StandardEnvironment();
            environment.getPropertySources().replace(
                    StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                    new MapPropertySource(
                            StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                            Map.of()
                    )
            );
            environment.getPropertySources().addFirst(new MapPropertySource(
                    "test",
                    Map.of("secret.value", encrypted)
            ));

            assertThatThrownBy(() -> new ConfigDecryptEnvironmentPostProcessor(
                    new AesGcmConfigDecryptor(),
                    new ConfigDecryptKeyProvider(tempDir.resolve("missing-key"))
            ).postProcessEnvironment(environment, null))
                    .isInstanceOf(ConfigDecryptException.class)
                    .hasMessage("Encrypted configuration value requires EGON_CONFIG_DECRYPT_KEY");
        } finally {
            Arrays.fill(key, '\0');
        }
    }

    @Test
    void doesNotReadKeyFromApplicationPropertiesWhenSystemEnvironmentExists(@TempDir Path tempDir) throws Exception {
        String keyText = "12345678901234567890123456789012";
        char[] key = keyText.toCharArray();
        try {
            String encrypted = ConfigCipherCli.encrypt(key, "secret-value");
            ConfigurableEnvironment environment = new StandardEnvironment();
            environment.getPropertySources().replace(
                    StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                    new MapPropertySource(
                            StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                            Map.of()
                    )
            );
            environment.getPropertySources().addFirst(new MapPropertySource(
                    "test",
                    Map.of(
                            "EGON_CONFIG_DECRYPT_KEY", keyText,
                            "secret.value", encrypted
                    )
            ));

            assertThatThrownBy(() -> new ConfigDecryptEnvironmentPostProcessor(
                    new AesGcmConfigDecryptor(),
                    new ConfigDecryptKeyProvider(tempDir.resolve("missing-key"))
            ).postProcessEnvironment(environment, null))
                    .isInstanceOf(ConfigDecryptException.class)
                    .hasMessage("Encrypted configuration value requires EGON_CONFIG_DECRYPT_KEY");
        } finally {
            Arrays.fill(key, '\0');
        }
    }

    private static class TrackingKeyProvider extends ConfigDecryptKeyProvider {

        private final Optional<char[]> key;
        private int resolveCount;

        private TrackingKeyProvider(Optional<char[]> key) {
            this.key = key;
        }

        @Override
        public Optional<char[]> resolveKey(ConfigurableEnvironment environment) {
            resolveCount++;
            return key.map(value -> value.clone());
        }

        @Override
        public Optional<char[]> resolveKey() {
            throw new AssertionError("Expected environment-aware key resolution");
        }

        private int resolveCount() {
            return resolveCount;
        }
    }

    @SpringBootConfiguration
    static class TestApplication {
    }
}
