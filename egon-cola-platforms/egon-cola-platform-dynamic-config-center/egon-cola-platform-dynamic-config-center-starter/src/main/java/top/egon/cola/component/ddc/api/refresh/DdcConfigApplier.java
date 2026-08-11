package top.egon.cola.component.ddc.api.refresh;

import org.springframework.lang.Nullable;

/**
 * 将单个配置键值应用到运行中组件的扩展接口。
 * Extension point that applies one configuration key-value pair to a running component.
 */
@FunctionalInterface
public interface DdcConfigApplier {

    /**
     * 应用指定版本的配置值。
     * Applies a configuration value at the specified version.
     *
     * @param key     配置键; configuration key
     * @param value   属性源原始字符串值，键被删除时可为 {@code null}; raw property-source string value, possibly {@code null} when removed
     * @param version 配置版本; configuration version
     */
    void apply(String key, @Nullable String value, long version);

    /**
     * 返回应用器优先级，数值越小越先执行。
     * Returns the applier priority; lower values execute first.
     *
     * @return 应用器优先级; applier priority
     */
    default int priority() {
        return 0;
    }
}
