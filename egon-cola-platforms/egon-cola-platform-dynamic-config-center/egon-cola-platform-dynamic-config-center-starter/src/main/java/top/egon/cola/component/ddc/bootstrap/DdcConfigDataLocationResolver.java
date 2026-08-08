package top.egon.cola.component.ddc.bootstrap;

import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationNotFoundException;
import org.springframework.boot.context.config.ConfigDataLocationResolver;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.boot.context.properties.bind.Bindable;
import top.egon.cola.component.ddc.config.DdcProperties;

import java.util.List;

/**
 * 解析 {@code ddc:application.yml} ConfigData 位置并注册引导客户端。 Resolves the {@code ddc:application.yml} ConfigData location and registers the bootstrap client.
 */
public final class DdcConfigDataLocationResolver
        implements ConfigDataLocationResolver<DdcConfigDataResource> {

    /**
     * DDC ConfigData 位置前缀。 DDC ConfigData location prefix.
     */
    public static final String PREFIX = "ddc:";

    /**
     * Starter 唯一支持的远程资源名。 The only remote resource name supported by the starter.
     */
    public static final String RESOURCE_NAME = "application.yml";

    /**
     * 用于引导阶段绑定配置的属性前缀。 Property prefix used for bootstrap binding.
     */
    private static final String PROPERTIES_PREFIX = "egon.cola.component.ddc";

    /**
     * 判断位置是否使用 DDC 前缀。 Determines whether the location uses the DDC prefix.
     *
     * @param context  ConfigData 解析上下文。 ConfigData resolution context
     * @param location 待判断的位置。 location to inspect
     * @return 位置可由本解析器处理时为 {@code true}。 {@code true} when this resolver can handle the location
     */
    @Override
    public boolean isResolvable(ConfigDataLocationResolverContext context,
                                ConfigDataLocation location) {
        return location != null && location.hasPrefix(PREFIX);
    }

    /**
     * 校验位置和客户端属性，并构造唯一 DDC ConfigData 资源。 Validates the location and client properties and creates the single DDC ConfigData resource.
     *
     * @param context  ConfigData 解析上下文。 ConfigData resolution context
     * @param location 待解析的位置。 location to resolve
     * @return DDC 禁用时为空，否则包含一个资源。 an empty list when DDC is disabled, otherwise one resource
     * @throws ConfigDataLocationNotFoundException 位置解析失败时抛出。 thrown when the location cannot be resolved
     * @throws ConfigDataResourceNotFoundException 资源解析失败时抛出。 thrown when the resource cannot be resolved
     * @throws IllegalArgumentException            资源名或必需属性无效时抛出。 thrown when the resource name or required properties are invalid
     */
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

    /**
     * 校验远程作用域、YAML 大小限制和管理端连接配置。 Validates the remote scope, YAML size limit, and management connection settings.
     *
     * @param properties 待校验的 DDC 属性。 DDC properties to validate
     * @throws IllegalArgumentException 任一必需设置无效时抛出。 thrown when any required setting is invalid
     */
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

    /**
     * 要求属性值包含非空白文本。 Requires a property value to contain non-whitespace text.
     *
     * @param value 属性值。 property value
     * @param name  用于错误消息的属性后缀。 property suffix used in error messages
     * @throws IllegalArgumentException 值为空或空白时抛出。 thrown when the value is null or blank
     */
    private void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    PROPERTIES_PREFIX + '.' + name + " is required"
            );
        }
    }
}
