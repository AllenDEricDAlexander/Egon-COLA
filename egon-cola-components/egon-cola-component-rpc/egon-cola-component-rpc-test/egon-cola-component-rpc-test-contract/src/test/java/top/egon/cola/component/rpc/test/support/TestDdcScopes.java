package top.egon.cola.component.rpc.test.support;

import top.egon.cola.component.ddc.config.DdcProperties;
import top.egon.cola.component.ddc.registry.DdcServiceKeyFactory;

public final class TestDdcScopes {

    private TestDdcScopes() {
    }

    public static DdcServiceKeyFactory serviceKeyFactory() {
        DdcProperties properties = new DdcProperties();
        properties.setBizCode("test-biz");
        properties.setAppCode("test-app");
        return new DdcServiceKeyFactory(properties);
    }
}
