package top.egon.cola.component.ddc.admin.service.config;

import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.format.DdcConfigFormatStrategyRegistry;
import top.egon.cola.component.ddc.format.DdcYamlConfigFormatStrategy;
import top.egon.cola.component.ddc.model.enums.DdcConfigFormat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 使用共享配置格式策略校验管理端接收的 YAML 内容和大小限制。
 * Validates YAML content and size limits received by the administration service using shared format strategies.
 */
public final class DdcYamlConfigValidator {

    /**
     * 允许的配置内容 UTF-8 最大字节数。 Maximum allowed configuration-content size in UTF-8 bytes.
     */
    private final long maxValueBytes;

    /**
     * 选择 YAML 解析实现的配置格式策略注册表。
     * Configuration-format strategy registry selecting the YAML parser implementation.
     */
    private final DdcConfigFormatStrategyRegistry formatStrategies;

    /**
     * 使用共享 YAML-only 策略注册表创建校验器。
     * Creates a validator with the shared YAML-only strategy registry.
     *
     * @param maxValueBytes 允许的配置内容 UTF-8 最大字节数; maximum allowed configuration-content size in UTF-8 bytes
     */
    public DdcYamlConfigValidator(long maxValueBytes) {
        this(maxValueBytes, DdcConfigFormatStrategyRegistry.defaults());
    }

    /**
     * 使用指定大小限制和格式策略注册表创建校验器。
     * Creates a validator with the specified size limit and format-strategy registry.
     *
     * @param maxValueBytes   允许的配置内容 UTF-8 最大字节数; maximum allowed configuration-content size in UTF-8 bytes
     * @param formatStrategies 配置格式策略注册表; configuration-format strategy registry
     */
    public DdcYamlConfigValidator(
            long maxValueBytes,
            DdcConfigFormatStrategyRegistry formatStrategies) {
        if (maxValueBytes <= 0) {
            throw new IllegalArgumentException("maxValueBytes must be positive");
        }
        this.maxValueBytes = maxValueBytes;
        this.formatStrategies = Objects.requireNonNull(
                formatStrategies,
                "formatStrategies"
        );
    }

    /**
     * 校验内容大小，并通过 YAML 策略执行结构与保留键校验。
     * Validates content size and delegates structure and reserved-key checks to the YAML strategy.
     *
     * @param content YAML 配置内容; YAML configuration content
     * @throws DdcAdminException 内容超限或 YAML 无效时抛出; thrown when content exceeds the limit or YAML is invalid
     */
    public void validate(String content) {
        long bytes = content == null
                ? 0
                : content.getBytes(StandardCharsets.UTF_8).length;
        if (bytes > maxValueBytes) {
            throw new DdcAdminException(
                    "application.yml exceeds the UTF-8 limit of "
                            + maxValueBytes + " bytes"
            );
        }
        try {
            formatStrategies.get(DdcConfigFormat.YAML).load(
                    DdcYamlConfigFormatStrategy.DEFAULT_RESOURCE_NAME,
                    content,
                    1L
            );
        } catch (IOException | RuntimeException exception) {
            throw new DdcAdminException(
                    "invalid application.yml: " + exception.getMessage()
            );
        }
    }
}
