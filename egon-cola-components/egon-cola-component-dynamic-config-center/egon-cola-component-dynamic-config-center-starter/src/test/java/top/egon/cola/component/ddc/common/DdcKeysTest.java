package top.egon.cola.component.ddc.common;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.model.enums.DdcLeaseRole;

import static org.assertj.core.api.Assertions.assertThat;

class DdcKeysTest {

    @Test
    void buildsConfigKeys() {
        assertThat(DdcKeys.config("demo", "dev", "default", "switch"))
                .isEqualTo("ddc:config:demo:dev:default:switch");
        assertThat(DdcKeys.version("demo", "dev", "default", "switch"))
                .isEqualTo("ddc:version:demo:dev:default:switch");
        assertThat(DdcKeys.topic("demo", "dev", "default"))
                .isEqualTo("ddc:topic:demo:dev:default");
    }

    @Test
    void buildsInstanceAndPublishKeys() {
        assertThat(DdcKeys.instance("demo", "dev", "default", "i1"))
                .isEqualTo("ddc:instance:demo:dev:default:i1");
        assertThat(DdcKeys.instances("demo", "dev", "default"))
                .isEqualTo("ddc:instances:demo:dev:default");
        assertThat(DdcKeys.leaseInstance("dev", "default", DdcLeaseRole.CONFIG_CLIENT, "i1"))
                .isEqualTo("ddc:lease:instance:dev:default:CONFIG_CLIENT:i1");
        assertThat(DdcKeys.publish("c1")).isEqualTo("ddc:publish:c1");
        assertThat(DdcKeys.publishAck("c1")).isEqualTo("ddc:publish:ack:c1");
    }
}
