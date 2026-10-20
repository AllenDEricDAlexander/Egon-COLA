package top.egon.cola.component.yuheng.admin.config;

import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;

public final class GatewayPersistenceTestSupport {

    private GatewayPersistenceTestSupport() {
    }

    public static GatewayPersistenceContextComponent persistenceContext() {
        GatewayPersistenceProperties properties = new GatewayPersistenceProperties()
                .setTenantId(7001L)
                .setIdentityTenantId(9001L)
                .setServiceUserId("scheduled-test-service");
        return new GatewayPersistenceContextComponent(properties, new EgonColaMybatisPlusProperties());
    }
}
