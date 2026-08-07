package top.egon.cola.component.ddc.bootstrap;

import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoader;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import top.egon.cola.component.ddc.environment.DdcDynamicPropertySource;
import top.egon.cola.component.ddc.environment.DdcYamlPropertySourceLoader;
import top.egon.cola.component.ddc.model.vo.DdcConfigValue;

import java.io.IOException;
import java.util.List;

public final class DdcConfigDataLoader
        implements ConfigDataLoader<DdcConfigDataResource> {

    private final DdcYamlPropertySourceLoader yamlLoader =
            new DdcYamlPropertySourceLoader();

    @Override
    public ConfigData load(ConfigDataLoaderContext context,
                           DdcConfigDataResource resource)
            throws IOException, ConfigDataResourceNotFoundException {
        DdcBootstrapClient client = context.getBootstrapContext()
                .get(DdcBootstrapClient.class);
        DdcConfigValue value = client.load(resource.resourceName());
        if (value == null) {
            if (resource.optional()) {
                return configData(yamlLoader.empty(
                        resource.resourceName()
                ));
            }
            throw new ConfigDataResourceNotFoundException(resource);
        }
        DdcDynamicPropertySource propertySource = yamlLoader.load(
                resource.resourceName(),
                value.getConfigValue(),
                value.getVersion()
        );
        return configData(propertySource);
    }

    private ConfigData configData(
            DdcDynamicPropertySource propertySource) {
        return new ConfigData(
                List.of(propertySource),
                ConfigData.Option.IGNORE_IMPORTS,
                ConfigData.Option.IGNORE_PROFILES
        );
    }
}
