package top.egon.cola.component.ddc.service;

@FunctionalInterface
public interface DdcConfigApplier {

    void apply(String key, String value, long version);
}
