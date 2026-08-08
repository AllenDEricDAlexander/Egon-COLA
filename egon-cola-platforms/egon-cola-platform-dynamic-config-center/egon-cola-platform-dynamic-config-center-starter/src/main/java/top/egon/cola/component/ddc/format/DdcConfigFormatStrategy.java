package top.egon.cola.component.ddc.format;

import top.egon.cola.component.ddc.environment.DdcDynamicPropertySource;
import top.egon.cola.component.ddc.model.enums.DdcConfigFormat;

import java.io.IOException;

/**
 * 将一种配置内容格式解析为 DDC 动态属性源的策略接口。
 * Strategy interface that parses one configuration content format into a DDC dynamic property source.
 *
 * <p>该接口是新增配置文件类型的唯一扩展点。调用方只通过格式注册表选择策略，不感知具体解析器。</p>
 * <p>This interface is the sole extension point for additional configuration file types. Callers select a
 * strategy through the format registry without depending on a concrete parser.</p>
 */
public interface DdcConfigFormatStrategy {

    /**
     * 返回策略处理的配置格式。 Returns the configuration format handled by this strategy.
     *
     * @return 配置格式; configuration format
     */
    DdcConfigFormat format();

    /**
     * 判断策略是否支持指定资源名。
     * Determines whether this strategy supports the specified resource name.
     *
     * @param resourceName 配置资源名; configuration resource name
     * @return 资源名与策略格式匹配时为 {@code true}; {@code true} when the resource name matches the strategy format
     */
    default boolean supports(String resourceName) {
        return format().supports(resourceName);
    }

    /**
     * 创建版本为零且无属性的动态属性源。
     * Creates a version-zero dynamic property source with no properties.
     *
     * @param resourceName 配置资源名; configuration resource name
     * @return 空动态属性源; empty dynamic property source
     */
    DdcDynamicPropertySource empty(String resourceName);

    /**
     * 解析并校验配置内容。
     * Parses and validates configuration content.
     *
     * @param resourceName 配置资源名; configuration resource name
     * @param content      配置文本; configuration text
     * @param version      配置版本; configuration version
     * @return 动态属性源; dynamic property source
     * @throws IOException 解析器读取资源失败时抛出; thrown when the parser cannot read the resource
     */
    DdcDynamicPropertySource load(String resourceName,
                                  String content,
                                  long version) throws IOException;
}
