package top.egon.cola.component.ddc.bootstrap;

import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationNotFoundException;
import org.springframework.boot.context.config.ConfigDataLocationResolver;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.boot.context.properties.bind.Bindable;
import top.egon.cola.component.ddc.config.DdcProperties;

import java.util.List;

public final class DdcConfigDataLocationResolver
        implements ConfigDataLocationResolver<DdcConfigDataResource> {

    public static final String PREFIX = "ddc:";

    public static final String RESOURCE_NAME = "application.yml";

    private static final String PROPERTIES_PREFIX = "egon.cola.component.ddc";

    @Override
    public boolean isResolvable(ConfigDataLocationResolverContext context,
                                ConfigDataLocation location) {
        return location != null && location.hasPrefix(PREFIX);
    }

    @Override
    public List<DdcConfigDataResource> resolve(
            ConfigDataLocationResolverContext context,
            ConfigDataLocation location)
            throws ConfigDataLocationNotFoundException,
            ConfigDataResourceNotFoundException {
        String resourceName = location.getNonPrefixedValue(PREFIX).trim();
        if (!RESOURCE_NAME.equals(resourceName)) {
            throw new IllegalArgumentException(
                    "DDC ConfigData only supports ddc:" + RESOURCE_NAME
            );
        }
        DdcProperties properties = context.getBinder()
                .bind(PROPERTIES_PREFIX, Bindable.of(DdcProperties.class))
                .orElseGet(DdcProperties::new);
        if (!properties.isEnabled()) {
            return List.of();
        }
        validate(properties);
        context.getBootstrapContext().registerIfAbsent(
                DdcBootstrapClient.class,
                bootstrapContext -> new DdcBootstrapClient(properties)
        );
        return List.of(new DdcConfigDataResource(
                location.isOptional(),
                properties.getBizCode(),
                properties.getEnv(),
                properties.getNamespace(),
                properties.getAppCode(),
                resourceName
        ));
    }

    private void validate(DdcProperties properties) {
        requireText(properties.getBizCode(), "biz-code");
        requireText(properties.getEnv(), "env");
        requireText(properties.getNamespace(), "namespace");
        requireText(properties.getAppCode(), "app-code");
        if (properties.getMaxYamlBytes() <= 0) {
            throw new IllegalArgumentException(
                    PROPERTIES_PREFIX + ".max-yaml-bytes must be positive"
            );
        }
        properties.getAdmin().requireEndpoint();
        properties.getAdmin().validateCredentials();
    }

    private void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    PROPERTIES_PREFIX + '.' + name + " is required"
            );
        }
    }
}
