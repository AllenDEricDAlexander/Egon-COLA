package top.egon.cola.component.ddc.configuration.bootstrap;

import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoader;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import top.egon.cola.component.ddc.configuration.environment.DdcDynamicPropertySource;
import top.egon.cola.component.ddc.format.DdcConfigFormatStrategy;
import top.egon.cola.component.ddc.format.DdcConfigFormatStrategyRegistry;
import top.egon.cola.component.ddc.model.config.DdcConfigValue;

import java.io.IOException;
import java.util.List;

/**
 * 将 DDC ConfigData 资源加载为动态 Spring 属性源。 Loads a DDC ConfigData resource as a dynamic Spring property source.
 */
public final class DdcConfigDataLoader
        implements ConfigDataLoader<DdcConfigDataResource> {

    /**
     * 负责按内容格式选择解析策略的注册表。 Registry that selects a parsing strategy by content format.
     */
    private final DdcConfigFormatStrategyRegistry formatStrategies =
            DdcConfigFormatStrategyRegistry.defaults();

    /**
     * 从引导上下文取得 DDC 客户端并加载配置数据。 Obtains the DDC client from the bootstrap context and loads configuration data.
     *
     * @param context  ConfigData 加载上下文。 ConfigData loading context
     * @param resource 待加载的 DDC 资源。 DDC resource to load
     * @return 包含动态属性源且忽略远程导入和 Profile 的配置数据。 configuration data containing the dynamic source while ignoring remote imports and profiles
     * @throws IOException                         YAML 资源读取失败时抛出。 thrown when the YAML resource cannot be read
     * @throws ConfigDataResourceNotFoundException 必选资源在 DDC 中不存在时抛出。 thrown when a required resource is absent from DDC
     */
    @Override
    public ConfigData load(ConfigDataLoaderContext context,
                           DdcConfigDataResource resource)
            throws IOException, ConfigDataResourceNotFoundException {
        DdcConfigDataFetcher client = context.getBootstrapContext()
                .get(DdcConfigDataFetcher.class);
        DdcConfigValue value = client.load(resource.resourceName());
        if (value == null) {
            if (resource.optional()) {
                DdcConfigFormatStrategy strategy =
                        formatStrategies.getByResourceName(
                                resource.resourceName()
                        );
                return configData(strategy.empty(resource.resourceName()));
            }
            throw new ConfigDataResourceNotFoundException(resource);
        }
        DdcConfigFormatStrategy strategy = formatStrategies.get(
                value.getFormat(),
                resource.resourceName()
        );
        DdcDynamicPropertySource propertySource = strategy.load(
                resource.resourceName(),
                value.getContent(),
                value.getVersion()
        );
        return configData(propertySource);
    }

    /**
     * 将动态属性源包装为受限的 ConfigData。 Wraps the dynamic property source in constrained ConfigData.
     *
     * @param propertySource DDC 动态属性源。 DDC dynamic property source
     * @return 禁止继续导入和 Profile 处理的 ConfigData。 ConfigData that suppresses further imports and profile processing
     */
    private ConfigData configData(
            DdcDynamicPropertySource propertySource) {
        return new ConfigData(
                List.of(propertySource),
                ConfigData.Option.IGNORE_IMPORTS,
                ConfigData.Option.IGNORE_PROFILES
        );
    }
}
