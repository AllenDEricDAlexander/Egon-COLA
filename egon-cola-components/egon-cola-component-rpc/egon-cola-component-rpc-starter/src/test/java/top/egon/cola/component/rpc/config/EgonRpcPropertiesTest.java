package top.egon.cola.component.rpc.config;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.provider.RpcProviderRegistrationMode;

import static org.assertj.core.api.Assertions.assertThat;

class EgonRpcPropertiesTest {

    @Test
    void providerRegistrationIsRequiredByDefault() {
        assertThat(new EgonRpcProperties()
                .getProvider()
                .getRegistrationMode())
                .isEqualTo(RpcProviderRegistrationMode.REQUIRED);
    }

    @Test
    void rpcIdentityUsesRpcOwnedDefaults() {
        EgonRpcProperties.Identity identity =
                new EgonRpcProperties().getIdentity();

        assertThat(identity.getEnv()).isEqualTo("default");
        assertThat(identity.getHost()).isNull();
        assertThat(identity.getInstanceId()).isNull();
    }

    @Test
    void consumerDefaultsToGatewayEngineServiceIdentity() {
        assertThat(new EgonRpcProperties()
                .getConsumer()
                .getGatewayServiceName())
                .isEqualTo("egon-gateway-rpc");
    }
}
