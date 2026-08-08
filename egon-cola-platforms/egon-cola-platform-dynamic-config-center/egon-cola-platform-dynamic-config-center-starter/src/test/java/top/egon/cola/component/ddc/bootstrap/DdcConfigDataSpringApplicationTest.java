package top.egon.cola.component.ddc.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertySource;
import top.egon.cola.component.ddc.config.DdcAutoConfig;
import top.egon.cola.component.ddc.config.DdcRegistryAutoConfig;
import top.egon.cola.component.ddc.model.vo.DdcConfigValue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DdcConfigDataSpringApplicationTest {

    @TempDir
    Path tempDirectory;

    @Test
    void remoteYamlOverridesLocalConfigDataBeforeBeanBinding() throws Exception {
        Path bootstrap = bootstrapFile();

        try (ConfigurableApplicationContext context = run(
                bootstrap,
                "remote"
        )) {
            SampleProperties properties = context.getBean(
                    SampleProperties.class
            );
            assertThat(properties.getValue()).isEqualTo("remote");
            assertThat(context.getEnvironment().getProperty("sample.value"))
                    .isEqualTo("remote");

            List<String> names = propertySourceNames(context);
            int ddcIndex = names.indexOf("ddc:application.yml");
            int bootstrapIndex = indexContaining(names, "bootstrap.yml");
            assertThat(ddcIndex).isGreaterThanOrEqualTo(0);
            assertThat(bootstrapIndex).isGreaterThan(ddcIndex);
        }
    }

    @Test
    void commandLineRetainsOfficialPrecedenceOverRemoteYaml()
            throws Exception {
        Path bootstrap = bootstrapFile();

        try (ConfigurableApplicationContext context = run(
                bootstrap,
                "remote",
                "--sample.value=command-line"
        )) {
            assertThat(context.getBean(SampleProperties.class).getValue())
                    .isEqualTo("command-line");
            assertThat(context.getEnvironment().getProperty("sample.value"))
                    .isEqualTo("command-line");
            assertThat(propertySourceNames(context))
                    .contains("ddc:application.yml");
        }
    }

    private ConfigurableApplicationContext run(Path bootstrap,
                                               String remoteValue,
                                               String... extraArguments) {
        DdcConfigValue value = new DdcConfigValue();
        value.setResourceName("application.yml");
        value.setFormat("YAML");
        value.setContent("sample:\n  value: " + remoteValue + "\n");
        value.setVersion(1L);
        DdcBootstrapClient client = new DdcBootstrapClient(
                () -> List.of(value),
                1024
        );

        SpringApplication application = new SpringApplication(
                TestApplication.class
        );
        application.setWebApplicationType(WebApplicationType.NONE);
        application.addBootstrapRegistryInitializer(registry ->
                registry.register(
                        DdcBootstrapClient.class,
                        BootstrapRegistry.InstanceSupplier.of(client)
                ));
        List<String> arguments = new ArrayList<>();
        arguments.add(
                "--spring.config.additional-location="
                        + bootstrap.toUri()
        );
        arguments.add(
                "--spring.autoconfigure.exclude="
                        + DdcAutoConfig.class.getName() + ','
                        + DdcRegistryAutoConfig.class.getName()
        );
        arguments.addAll(List.of(extraArguments));
        return application.run(arguments.toArray(String[]::new));
    }

    private Path bootstrapFile() throws Exception {
        Path bootstrap = tempDirectory.resolve("bootstrap.yml");
        Files.writeString(bootstrap, """
                spring:
                  config:
                    import: ddc:application.yml
                egon:
                  cola:
                    component:
                      ddc:
                        enabled: true
                        biz-code: orders
                        env: test
                        namespace: default
                        app-code: order-service
                        admin:
                          endpoint: http://ddc.test
                          signature-enabled: false
                sample:
                  value: bootstrap
                """);
        return bootstrap;
    }

    private List<String> propertySourceNames(
            ConfigurableApplicationContext context) {
        List<String> names = new ArrayList<>();
        for (PropertySource<?> propertySource
                : context.getEnvironment().getPropertySources()) {
            names.add(propertySource.getName());
        }
        return names;
    }

    private int indexContaining(List<String> values, String expected) {
        for (int index = 0; index < values.size(); index++) {
            if (values.get(index).contains(expected)) {
                return index;
            }
        }
        return -1;
    }

    @SpringBootConfiguration
    @EnableConfigurationProperties(SampleProperties.class)
    static class TestApplication {
    }

    @ConfigurationProperties("sample")
    public static class SampleProperties {

        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
