package top.egon.cola.component.rpc.config;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.provider.registration.RpcProviderRegistrationMode;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void consumerSharedTransportSettingsArePositive() {
        EgonRpcProperties.Consumer consumer =
                new EgonRpcProperties().getConsumer();

        consumer.validateSharedSettings();

        consumer.setDefaultTimeoutMs(0);
        assertInvalidSharedSettings(consumer);
        consumer.setDefaultTimeoutMs(3000);
        consumer.setChannelDrainTimeoutMs(0);
        assertInvalidSharedSettings(consumer);
    }

    @Test
    void consumerHasNoAutomaticGatewayDirectFallbackProperty() {
        assertThat(Arrays.stream(
                EgonRpcProperties.Consumer.class.getDeclaredFields()
        ).map(field -> field.getName().toLowerCase()))
                .noneMatch(name -> name.contains("mode"))
                .noneMatch(name -> name.contains("fallback"));
    }

    private void assertInvalidSharedSettings(
            EgonRpcProperties.Consumer consumer) {
        assertThatThrownBy(consumer::validateSharedSettings)
                .isInstanceOfSatisfying(EgonRpcException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(
                                EgonRpcErrorCode.RPC_INVALID_CONTRACT
                        ));
    }
}
