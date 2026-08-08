package top.egon.cola.component.ddc.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultBootstrapContext;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DdcConfigDataLocationResolverTest {

    private final DdcConfigDataLocationResolver resolver =
            new DdcConfigDataLocationResolver();

    @Test
    void resolvabilityCheckDoesNotAccessContext() {
        ConfigDataLocationResolverContext context =
                mock(ConfigDataLocationResolverContext.class);

        assertThat(resolver.isResolvable(
                context,
                ConfigDataLocation.of("ddc:application.yml")
        )).isTrue();
        assertThat(resolver.isResolvable(
                context,
                ConfigDataLocation.of("classpath:application.yml")
        )).isFalse();
        verifyNoInteractions(context);
    }

    @Test
    void resolvesLocalBootstrapSettingsAndRegistersClient() {
        DefaultBootstrapContext bootstrapContext =
                new DefaultBootstrapContext();
        ConfigDataLocationResolverContext context = context(
                bootstrapContext,
                Map.of(
                        "egon.cola.component.ddc.enabled", true,
                        "egon.cola.component.ddc.biz-code", "orders",
                        "egon.cola.component.ddc.env", "test",
                        "egon.cola.component.ddc.namespace", "default",
                        "egon.cola.component.ddc.app-code", "order-service",
                        "egon.cola.component.ddc.admin.endpoint", "http://ddc.test",
                        "egon.cola.component.ddc.admin.signature-enabled", false
                )
        );

        List<DdcConfigDataResource> resources = resolver.resolve(
                context,
                ConfigDataLocation.of("optional:ddc:application.yml")
        );

        assertThat(resources).containsExactly(new DdcConfigDataResource(
                true,
                "orders",
                "test",
                "default",
                "order-service",
                "application.yml"
        ));
        assertThat(bootstrapContext.isRegistered(DdcBootstrapClient.class))
                .isTrue();
    }

    @Test
    void disabledDdcContributesNoResource() {
        DefaultBootstrapContext bootstrapContext =
                new DefaultBootstrapContext();
        ConfigDataLocationResolverContext context = context(
                bootstrapContext,
                Map.of("egon.cola.component.ddc.enabled", false)
        );

        assertThat(resolver.resolve(
                context,
                ConfigDataLocation.of("ddc:application.yml")
        )).isEmpty();
        assertThat(bootstrapContext.isRegistered(DdcBootstrapClient.class))
                .isFalse();
    }

    @Test
    void acceptsApplicationYamlNamesAndRejectsOtherResources() {
        ConfigDataLocationResolverContext context = context(
                new DefaultBootstrapContext(),
                Map.of("egon.cola.component.ddc.enabled", false)
        );

        assertThat(resolver.resolve(
                context,
                ConfigDataLocation.of("ddc:application.yml")
        )).isEmpty();
        assertThat(resolver.resolve(
                context,
                ConfigDataLocation.of("ddc:application.yaml")
        )).isEmpty();

        assertThatThrownBy(() -> resolver.resolve(
                context,
                ConfigDataLocation.of("ddc:feature.yaml")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "DDC ConfigData only supports YAML resources: feature.yaml"
                );
    }

    private ConfigDataLocationResolverContext context(
            DefaultBootstrapContext bootstrapContext,
            Map<String, Object> properties) {
        ConfigDataLocationResolverContext context =
                mock(ConfigDataLocationResolverContext.class);
        Binder binder = new Binder(
                new MapConfigurationPropertySource(properties)
        );
        when(context.getBinder()).thenReturn(binder);
        when(context.getBootstrapContext()).thenReturn(bootstrapContext);
        return context;
    }
}
