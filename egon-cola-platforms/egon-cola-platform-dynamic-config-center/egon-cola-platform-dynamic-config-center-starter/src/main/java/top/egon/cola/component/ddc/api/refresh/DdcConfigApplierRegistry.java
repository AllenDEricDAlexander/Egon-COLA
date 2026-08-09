package top.egon.cola.component.ddc.api.refresh;

/**
 * DDC 配置应用器注册表。
 * Registry of DDC configuration appliers.
 */
public interface DdcConfigApplierRegistry {

    /**
     * 按精确配置键注册应用器。
     * Registers an applier for an exact configuration key.
     *
     * @param configKey 精确配置键; exact configuration key
     * @param applier   配置应用器; configuration applier
     */
    void registerExact(String configKey, DdcConfigApplier applier);

    /**
     * 按配置键前缀注册应用器。
     * Registers an applier for a configuration key prefix.
     *
     * @param configKeyPrefix 配置键前缀; configuration key prefix
     * @param applier         配置应用器; configuration applier
     */
    void registerPrefix(String configKeyPrefix, DdcConfigApplier applier);

    /**
     * 解析用于处理指定配置键的应用器。
     * Resolves the applier that handles the specified configuration key.
     *
     * @param configKey 配置键; configuration key
     * @return 已解析的配置应用器; resolved configuration applier
     */
    DdcConfigApplier resolve(String configKey);

    /**
     * 判断指定配置键是否具有精确或前缀显式注册。
     * Indicates whether the key has an explicit exact or prefix registration.
     *
     * @param configKey 配置键; configuration key
     * @return 存在显式注册时为 {@code true}; {@code true} when an explicit registration exists
     */
    default boolean hasExplicitRegistration(String configKey) {
        return false;
    }
}
