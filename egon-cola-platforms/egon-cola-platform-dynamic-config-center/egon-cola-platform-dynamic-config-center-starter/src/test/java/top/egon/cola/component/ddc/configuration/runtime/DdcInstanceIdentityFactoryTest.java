package top.egon.cola.component.ddc.configuration.runtime;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.autoconfigure.DdcProperties;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DdcInstanceIdentityFactoryTest {

    @Test
    void configuredInstanceIdOverridesCustomProvider() {
        DdcProperties properties = properties();
        properties.getInstance().setId("pod-uid-1");

        var identity = new DdcInstanceIdentityFactory(
                properties,
                () -> "custom-instance-1"
        ).create();

        assertThat(identity.instanceId()).isEqualTo("pod-uid-1");
        assertThat(identity.bizCode()).isEqualTo("retail");
        assertThat(identity.env()).isEqualTo("local");
        assertThat(identity.appCode()).isEqualTo("order");
        assertThat(identity.namespace()).isNull();
    }

    @Test
    void customProviderOverridesDefaultUuidV7() {
        var identity = new DdcInstanceIdentityFactory(
                properties(),
                () -> "custom-instance-1"
        ).create();

        assertThat(identity.instanceId()).isEqualTo("custom-instance-1");
    }

    @Test
    void defaultsToCompleteUuidV7() {
        var identity = new DdcInstanceIdentityFactory(
                properties(),
                null
        ).create();

        UUID uuid = UUID.fromString(identity.instanceId());
        assertThat(uuid.version()).isEqualTo(7);
        assertThat(identity.instanceId()).hasSize(36);
    }

    private DdcProperties properties() {
        DdcProperties properties = new DdcProperties();
        properties.setBizCode("retail");
        properties.setAppCode("order");
        properties.setEnv("local");
        return properties;
    }
}
