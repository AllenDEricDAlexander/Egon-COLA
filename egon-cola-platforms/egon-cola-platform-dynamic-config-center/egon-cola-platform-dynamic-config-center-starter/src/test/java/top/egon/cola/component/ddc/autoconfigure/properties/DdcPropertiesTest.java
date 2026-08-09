package top.egon.cola.component.ddc.autoconfigure.properties;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcPropertiesTest {

    @Test
    void machineTransportPropertiesAreOwnedByTheRpcAdapter() {
        assertThat(Arrays.stream(DdcProperties.class.getDeclaredClasses())
                .map(Class::getSimpleName))
                .doesNotContain("Admin");
        assertThat(Arrays.stream(DdcProperties.class.getDeclaredMethods())
                .map(method -> method.getName()))
                .doesNotContain("getAdmin");
    }

    @Test
    void configClientHeartbeatMustBePositiveAndShorterThanLease() {
        DdcProperties.Instance instance = new DdcProperties.Instance();
        instance.setHeartbeatIntervalSeconds(0);
        instance.setLeaseSeconds(30);

        assertThatThrownBy(instance::validate)
                .hasMessage(
                        "egon.cola.component.ddc.instance.heartbeat-interval-seconds "
                                + "must be positive and less than lease-seconds"
                );

        instance.setHeartbeatIntervalSeconds(30);

        assertThatThrownBy(instance::validate)
                .hasMessage(
                        "egon.cola.component.ddc.instance.heartbeat-interval-seconds "
                                + "must be positive and less than lease-seconds"
                );

        instance.setHeartbeatIntervalSeconds(10);
        instance.validate();
    }
}
