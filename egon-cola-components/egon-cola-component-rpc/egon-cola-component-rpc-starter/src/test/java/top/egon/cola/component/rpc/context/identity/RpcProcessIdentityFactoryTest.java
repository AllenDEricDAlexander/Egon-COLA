package top.egon.cola.component.rpc.context.identity;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import top.egon.cola.component.rpc.config.EgonRpcProperties;

import static org.assertj.core.api.Assertions.assertThat;

class RpcProcessIdentityFactoryTest {

    @Test
    void createsIdentityFromRpcOwnedProperties() {
        EgonRpcProperties properties = new EgonRpcProperties();
        properties.getIdentity().setEnv("test");
        properties.getIdentity().setHost("127.0.0.1");
        properties.getIdentity().setInstanceId("orders-api-100");

        RpcProcessIdentity identity = new RpcProcessIdentityFactory(
                new MockEnvironment().withProperty("spring.application.name", "orders-api"),
                properties
        ).create();

        assertThat(identity.applicationName()).isEqualTo("orders-api");
        assertThat(identity.env()).isEqualTo("test");
        assertThat(identity.instanceId()).isEqualTo("orders-api-100");
        assertThat(identity.host()).isEqualTo("127.0.0.1");
        assertThat(identity.pid()).isEqualTo(ProcessHandle.current().pid());
    }

    @Test
    void derivesStableProcessDefaultsWithoutDdc() {
        RpcProcessIdentity identity = new RpcProcessIdentityFactory(
                new MockEnvironment().withProperty(
                        "spring.application.name",
                        "orders-api"
                ),
                new EgonRpcProperties()
        ).create();

        assertThat(identity.applicationName()).isEqualTo("orders-api");
        assertThat(identity.env()).isEqualTo("default");
        assertThat(identity.host()).isNotBlank();
        assertThat(identity.instanceId())
                .startsWith("orders-api-")
                .endsWith("-" + ProcessHandle.current().pid());
    }
}
