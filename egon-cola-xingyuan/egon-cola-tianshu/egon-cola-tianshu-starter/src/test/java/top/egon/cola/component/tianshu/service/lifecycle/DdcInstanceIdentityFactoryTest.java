package top.egon.cola.component.tianshu.service.lifecycle;

import java.time.Duration;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcProperties;
import static org.assertj.core.api.Assertions.assertThat;

class DdcInstanceIdentityFactoryTest {

    @BeforeAll
    static void bindTheProcessWideEngine() {
        SnowflakeIdGenerator.initialize(0L, Duration.ofMillis(5));
    }

    @Test
    void configuredInstanceIdOverridesCustomProvider() {
        DdcProperties properties = properties();
        properties.getInstance().setId("pod-uid-1");

        var identity = new DdcInstanceIdentityFactory(
                properties,
                () -> "custom-instance-1").create();

        assertThat(identity.instanceId()).isEqualTo("pod-uid-1");
        assertThat(identity.bizCode()).isEqualTo("retail");
        assertThat(identity.env()).isEqualTo("local");
        assertThat(identity.appCode()).isEqualTo("order");
        assertThat(identity.namespace()).isNull();
    }

    @Test
    void customProviderOverridesDefaultSnowflake() {
        var identity = new DdcInstanceIdentityFactory(
                properties(),
                () -> "custom-instance-1").create();

        assertThat(identity.instanceId()).isEqualTo("custom-instance-1");
    }

    @Test
    void defaultsToSnowflakeId() {
        var identity = new DdcInstanceIdentityFactory(
                properties(),
                null).create();

        assertThat(identity.instanceId()).matches("\\d+");
        assertThat(Long.parseLong(identity.instanceId())).isPositive();
    }

    private DdcProperties properties() {
        DdcProperties properties = new DdcProperties();
        properties.setBizCode("retail");
        properties.setAppCode("order");
        properties.setEnv("local");
        return properties;
    }
}
