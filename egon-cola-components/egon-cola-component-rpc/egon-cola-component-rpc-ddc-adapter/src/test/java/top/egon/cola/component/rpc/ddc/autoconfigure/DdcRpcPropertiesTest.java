package top.egon.cola.component.rpc.ddc.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcRpcPropertiesTest {

    @Test
    void bindsDirectTransportAndIndependentCredentials() {
        DdcRpcProperties properties = new Binder(
                new MapConfigurationPropertySource(Map.of(
                        "egon.cola.component.ddc.rpc.target", "dns:///ddc-admin:19080",
                        "egon.cola.component.ddc.rpc.connect-timeout", "2s",
                        "egon.cola.component.ddc.rpc.default-timeout", "7s",
                        "egon.cola.component.ddc.rpc.auth.runtime.access-key", "runtime-ak",
                        "egon.cola.component.ddc.rpc.auth.runtime.secret-key", "runtime-sk",
                        "egon.cola.component.ddc.rpc.auth.registry.access-key", "registry-ak",
                        "egon.cola.component.ddc.rpc.auth.registry.secret-key", "registry-sk",
                        "egon.cola.component.ddc.rpc.auth.management.access-key", "management-ak",
                        "egon.cola.component.ddc.rpc.auth.management.secret-key", "management-sk"
                )))
                .bind("egon.cola.component.ddc.rpc", DdcRpcProperties.class)
                .orElseThrow(AssertionError::new);

        assertThat(properties.requireTarget()).isEqualTo("dns:///ddc-admin:19080");
        assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(properties.getDefaultTimeout()).isEqualTo(Duration.ofSeconds(7));
        assertThat(properties.runtimeCredential().accessKey()).isEqualTo("runtime-ak");
        assertThat(properties.registryCredential().accessKey()).isEqualTo("registry-ak");
        assertThat(properties.managementCredential().accessKey()).isEqualTo("management-ak");
    }

    @Test
    void validatesOnlyCredentialProfileRequestedByCaller() {
        DdcRpcProperties properties = new DdcRpcProperties();
        properties.setTarget("localhost:19080");
        properties.getAuth().getRuntime().setAccessKey("runtime-ak");
        properties.getAuth().getRuntime().setSecretKey("runtime-sk");

        assertThat(properties.runtimeCredential().accessKey()).isEqualTo("runtime-ak");
        assertThatThrownBy(properties::registryCredential)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("registry.access-key");
    }

    @Test
    void requiresTargetOnlyWhenCreatingAClient() {
        DdcRpcProperties properties = new DdcRpcProperties();

        assertThatThrownBy(properties::requireTarget)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("egon.cola.component.ddc.rpc.target");
    }
}
