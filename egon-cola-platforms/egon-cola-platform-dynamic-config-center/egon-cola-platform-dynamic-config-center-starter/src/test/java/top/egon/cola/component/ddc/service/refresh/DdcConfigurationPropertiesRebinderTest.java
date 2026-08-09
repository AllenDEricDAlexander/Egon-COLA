package top.egon.cola.component.ddc.service.refresh;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import top.egon.cola.component.ddc.annotation.DdcRefreshable;
import top.egon.cola.component.ddc.environment.DdcDynamicPropertySource;
import top.egon.cola.component.ddc.format.DdcYamlConfigFormatStrategy;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcConfigurationPropertiesRebinderTest {

    @Test
    void rebindsMarkedJavaBeanAndUsesLowerPriorityFallback()
            throws Exception {
        DdcDynamicPropertySource source = source("""
                sample:
                  label: remote
                """, 1L);
        try (AnnotationConfigApplicationContext context = context(
                MutableConfig.class,
                source
        )) {
            context.getEnvironment().getPropertySources().addLast(
                    new MapPropertySource(
                            "local",
                            Map.of("sample.label", "local")
                    )
            );
            MutableProperties properties =
                    context.getBean(MutableProperties.class);
            assertThat(properties.getLabel()).isEqualTo("remote");
            DdcConfigurationPropertiesRebinder rebinder =
                    rebinder(context);

            source.replace(source("other: true\n", 2L).snapshot());
            Set<String> refreshed = rebinder.rebind(
                    Set.of("sample.label"),
                    Set.of("sample.label")
            );

            assertThat(properties.getLabel()).isEqualTo("local");
            assertThat(refreshed).containsExactly("sample.label");
        }
    }

    @Test
    void removalWithoutFallbackIsClassifiedForRestart()
            throws Exception {
        DdcDynamicPropertySource source = source("""
                sample:
                  label: remote
                """, 1L);
        try (AnnotationConfigApplicationContext context = context(
                MutableConfig.class,
                source
        )) {
            MutableProperties properties =
                    context.getBean(MutableProperties.class);
            DdcConfigurationPropertiesRebinder rebinder =
                    rebinder(context);

            source.replace(source("other: true\n", 2L).snapshot());
            Set<String> refreshed = rebinder.rebind(
                    Set.of("sample.label"),
                    Set.of("sample.label")
            );

            assertThat(refreshed).isEmpty();
            assertThat(properties.getLabel()).isEqualTo("remote");
        }
    }

    @Test
    void rejectsValueObjectConfigurationProperties() throws Exception {
        DdcDynamicPropertySource source = source("""
                immutable:
                  label: remote
                """, 1L);
        try (AnnotationConfigApplicationContext context = context(
                ImmutableConfig.class,
                source
        )) {
            DdcConfigurationPropertiesRebinder rebinder =
                    new DdcConfigurationPropertiesRebinder(
                            context,
                            context.getBean(
                                    ConfigurationPropertiesBindingPostProcessor.class
                            ),
                            context.getEnvironment()
                    );

            assertThatThrownBy(rebinder::afterSingletonsInstantiated)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must use JavaBean binding");
        }
    }

    private DdcConfigurationPropertiesRebinder rebinder(
            AnnotationConfigApplicationContext context) {
        DdcConfigurationPropertiesRebinder rebinder =
                new DdcConfigurationPropertiesRebinder(
                        context,
                        context.getBean(
                                ConfigurationPropertiesBindingPostProcessor.class
                        ),
                        context.getEnvironment()
                );
        rebinder.afterSingletonsInstantiated();
        return rebinder;
    }

    private AnnotationConfigApplicationContext context(
            Class<?> configuration,
            DdcDynamicPropertySource source) {
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(source);
        context.register(configuration);
        context.refresh();
        return context;
    }

    private DdcDynamicPropertySource source(String yaml, long version)
            throws Exception {
        return new DdcYamlConfigFormatStrategy().load(
                "application.yml",
                yaml,
                version
        );
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(MutableProperties.class)
    static class MutableConfig {
    }

    @DdcRefreshable
    @ConfigurationProperties("sample")
    static class MutableProperties {

        private String label;

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ImmutableProperties.class)
    static class ImmutableConfig {
    }

    @DdcRefreshable
    @ConfigurationProperties("immutable")
    record ImmutableProperties(String label) {
    }
}
