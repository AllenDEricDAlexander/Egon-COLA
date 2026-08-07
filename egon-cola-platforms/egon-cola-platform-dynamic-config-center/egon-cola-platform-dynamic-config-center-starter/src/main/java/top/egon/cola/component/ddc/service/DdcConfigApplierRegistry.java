package top.egon.cola.component.ddc.service;

public interface DdcConfigApplierRegistry {

    void registerExact(String configKey, DdcConfigApplier applier);

    void registerPrefix(String configKeyPrefix, DdcConfigApplier applier);

    DdcConfigApplier resolve(String configKey);

    default boolean hasExplicitRegistration(String configKey) {
        return false;
    }
}
