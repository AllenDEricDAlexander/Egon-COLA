package top.egon.cola.component.rpc.tianshu.configdata;

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
                ConfigDataLocation.of("tianshu:application.yml")
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
                        "egon.cola.component.tianshu.enabled", true,
                        "egon.cola.component.tianshu.biz-code", "orders",
                        "egon.cola.component.tianshu.env", "test",
                        "egon.cola.component.tianshu.namespace", "default",
                        "egon.cola.component.tianshu.app-code", "order-service",
                        "egon.cola.component.tianshu.rpc.target", "localhost:19080",
                        "egon.cola.component.tianshu.rpc.auth.enabled", false
                )
        );

        List<DdcConfigDataResource> resources = resolver.resolve(
                context,
                ConfigDataLocation.of("optional:tianshu:application.yml")
        );

        assertThat(resources).containsExactly(new DdcConfigDataResource(
                true,
                "orders",
                "test",
                "default",
                "order-service",
                "application.yml"
        ));
        assertThat(bootstrapContext.isRegistered(DdcConfigDataFetcher.class))
                .isTrue();
    }

    @Test
    void disabledDdcContributesNoResource() {
        DefaultBootstrapContext bootstrapContext =
                new DefaultBootstrapContext();
        ConfigDataLocationResolverContext context = context(
                bootstrapContext,
                Map.of("egon.cola.component.tianshu.enabled", false)
        );

        assertThat(resolver.resolve(
                context,
                ConfigDataLocation.of("tianshu:application.yml")
        )).isEmpty();
        assertThat(bootstrapContext.isRegistered(DdcConfigDataFetcher.class))
                .isFalse();
    }

    @Test
    void acceptsApplicationYamlNamesAndRejectsOtherResources() {
        ConfigDataLocationResolverContext context = context(
                new DefaultBootstrapContext(),
                Map.of("egon.cola.component.tianshu.enabled", false)
        );

        assertThat(resolver.resolve(
                context,
                ConfigDataLocation.of("tianshu:application.yml")
        )).isEmpty();
        assertThat(resolver.resolve(
                context,
                ConfigDataLocation.of("tianshu:application.yaml")
        )).isEmpty();

        assertThatThrownBy(() -> resolver.resolve(
                context,
                ConfigDataLocation.of("tianshu:feature.yaml")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Tianshu ConfigData only supports YAML resources: feature.yaml"
                );
    }

    @Test
    void requiredLocationRejectsMissingRpcBootstrapWhileOptionalContinues() {
        Map<String, Object> properties = Map.of(
                "egon.cola.component.tianshu.enabled", true,
                "egon.cola.component.tianshu.biz-code", "orders",
                "egon.cola.component.tianshu.env", "test",
                "egon.cola.component.tianshu.namespace", "default",
                "egon.cola.component.tianshu.app-code", "order-service"
        );

        assertThatThrownBy(() -> resolver.resolve(
                context(new DefaultBootstrapContext(), properties),
                ConfigDataLocation.of("tianshu:application.yml")
        )).hasMessageContaining("egon.cola.component.tianshu.rpc.target");
        assertThat(resolver.resolve(
                context(new DefaultBootstrapContext(), properties),
                ConfigDataLocation.of("optional:tianshu:application.yml")
        )).isEmpty();
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
