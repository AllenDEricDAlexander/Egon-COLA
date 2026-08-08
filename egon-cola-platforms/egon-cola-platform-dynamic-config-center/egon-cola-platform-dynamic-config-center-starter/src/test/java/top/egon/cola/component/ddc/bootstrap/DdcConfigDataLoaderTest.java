package top.egon.cola.component.ddc.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.DefaultBootstrapContext;
import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import top.egon.cola.component.ddc.environment.DdcDynamicPropertySource;
import top.egon.cola.component.ddc.model.vo.DdcConfigValue;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DdcConfigDataLoaderTest {

    private final DdcConfigDataLoader loader = new DdcConfigDataLoader();

    @Test
    void loadsDynamicPropertySourceWithConfigDataSafetyOptions()
            throws Exception {
        DdcConfigValue value = value("feature:\n  enabled: true\n", 4L);
        ConfigData configData = loader.load(
                context(new DdcBootstrapClient(
                        () -> List.of(value),
                        1024
                )),
                resource(false)
        );

        assertThat(configData.getPropertySources()).hasSize(1);
        DdcDynamicPropertySource propertySource =
                (DdcDynamicPropertySource) configData
                        .getPropertySources().getFirst();
        assertThat(propertySource.getProperty("feature.enabled"))
                .isEqualTo(true);
        assertThat(configData.getOptions(propertySource))
                .satisfies(options -> {
                    assertThat(options.contains(
                            ConfigData.Option.IGNORE_IMPORTS
                    )).isTrue();
                    assertThat(options.contains(
                            ConfigData.Option.IGNORE_PROFILES
                    )).isTrue();
                });
    }

    @Test
    void optionalMissingRemoteDocumentKeepsAnEmptyDynamicSource()
            throws Exception {
        ConfigDataLoaderContext context = context(new DdcBootstrapClient(
                List::<DdcConfigValue>of,
                1024
        ));

        ConfigData configData = loader.load(context, resource(true));

        assertThat(configData.getPropertySources()).singleElement()
                .isInstanceOfSatisfying(
                        DdcDynamicPropertySource.class,
                        source -> {
                            assertThat(source.getPropertyNames()).isEmpty();
                            assertThat(source.snapshot().version()).isZero();
                        }
                );
    }

    @Test
    void requiredMissingRemoteDocumentUsesBootNotFoundSemantics() {
        ConfigDataLoaderContext context = context(new DdcBootstrapClient(
                List::<DdcConfigValue>of,
                1024
        ));

        assertThatThrownBy(() -> loader.load(context, resource(false)))
                .isInstanceOf(ConfigDataResourceNotFoundException.class);
    }

    private ConfigDataLoaderContext context(DdcBootstrapClient client) {
        DefaultBootstrapContext bootstrapContext =
                new DefaultBootstrapContext();
        bootstrapContext.register(
                DdcBootstrapClient.class,
                BootstrapRegistry.InstanceSupplier.of(client)
        );
        ConfigDataLoaderContext context = mock(ConfigDataLoaderContext.class);
        when(context.getBootstrapContext()).thenReturn(bootstrapContext);
        return context;
    }

    private DdcConfigDataResource resource(boolean optional) {
        return new DdcConfigDataResource(
                optional,
                "orders",
                "test",
                "default",
                "order-service",
                "application.yml"
        );
    }

    private DdcConfigValue value(String content, long version) {
        DdcConfigValue value = new DdcConfigValue();
        value.setResourceName("application.yml");
        value.setFormat("YAML");
        value.setContent(content);
        value.setVersion(version);
        return value;
    }
}
