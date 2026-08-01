package top.egon.cola.component.rpc.context;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import top.egon.cola.component.ddc.config.DdcProperties;
import top.egon.cola.component.ddc.model.vo.DdcInstanceIdentity;

import static org.assertj.core.api.Assertions.assertThat;

class RpcProcessIdentityFactoryTest {

    @Test
    void reusesTheDdcRuntimeInstanceIdentity() {
        DdcProperties properties = new DdcProperties();
        properties.setAppCode("orders");
        properties.setEnv("test");
        DdcInstanceIdentity ddcIdentity = new DdcInstanceIdentity(
                "019f-runtime-instance",
                "orders",
                "test",
                "default",
                "127.0.0.1",
                null,
                "100",
                "5.3.2"
        );

        RpcProcessIdentity identity = new RpcProcessIdentityFactory(
                new MockEnvironment().withProperty("spring.application.name", "orders-api"),
                properties,
                ddcIdentity
        ).create();

        assertThat(identity.applicationName()).isEqualTo("orders-api");
        assertThat(identity.instanceId()).isEqualTo("019f-runtime-instance");
        assertThat(identity.host()).isEqualTo("127.0.0.1");
    }
}
