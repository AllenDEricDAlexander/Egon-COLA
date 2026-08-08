package top.egon.cola.component.ddc.model.enums;

import java.util.Locale;
import java.util.Set;

/**
 * 定义 DDC 配置资源支持的内容格式。
 * Defines content formats supported by DDC configuration resources.
 *
 * <p>当前版本只实现 YAML。新增格式时应同时增加对应的
 * {@code DdcConfigFormatStrategy}，不应在调用方增加格式分支。</p>
 *
 * <p>The current version implements YAML only. A new format must be accompanied
 * by a corresponding {@code DdcConfigFormatStrategy} instead of format branches
 * in callers.</p>
 */
public enum DdcConfigFormat {

    /**
     * YAML 配置格式，支持 {@code .yml} 和 {@code .yaml} 资源名。
     * YAML configuration format supporting {@code .yml} and {@code .yaml} resource names.
     */
    YAML(Set.of(".yml", ".yaml"));

    /**
     * 当前格式支持的资源名后缀。 Resource-name suffixes supported by this format.
     */
    private final Set<String> fileExtensions;

    /**
     * 创建配置格式定义。 Creates a configuration-format definition.
     *
     * @param fileExtensions 支持的资源名后缀; supported resource-name suffixes
     */
    DdcConfigFormat(Set<String> fileExtensions) {
        this.fileExtensions = Set.copyOf(fileExtensions);
    }

    /**
     * 判断资源名是否属于当前格式。
     * Determines whether a resource name belongs to this format.
     *
     * @param resourceName 配置资源名; configuration resource name
     * @return 资源名后缀匹配时为 {@code true}; {@code true} when the resource-name suffix matches
     */
    public boolean supports(String resourceName) {
        if (resourceName == null || resourceName.isBlank()) {
            return false;
        }
        String normalized = resourceName.trim().toLowerCase(Locale.ROOT);
        return fileExtensions.stream().anyMatch(normalized::endsWith);
    }

    /**
     * 将外部格式名称解析为枚举值。
     * Parses an external format name into an enum value.
     *
     * @param value 外部格式名称; external format name
     * @return 配置格式; configuration format
     * @throws IllegalArgumentException 名称为空或当前版本不支持时抛出; thrown when the name is blank or unsupported
     */
    public static DdcConfigFormat from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("DDC config format must not be blank");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Unsupported DDC config format: " + value,
                    exception
            );
        }
    }
}
