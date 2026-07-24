package top.egon.cola.component.ddc.model.registry;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcServiceRegistrationTest {

    @Test
    void shouldAcceptOnlyKnownRpcFrameworkMetadata() {
        DdcServiceRegistration registration = registration(Map.of(
                "egon.rpc.transport", "grpc",
                "egon.rpc.serialization", "protobuf",
                "egon.rpc.runtime-version", "5.2.3"
        ));

        assertThat(registration.metadata()).hasSize(3);
        assertThatThrownBy(() -> registration(Map.of(
                "egon.rpc.transport", "http"
        ))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registration(Map.of(
                "egon.rpc.provider-address", "127.0.0.1"
        ))).isInstanceOf(IllegalArgumentException.class);
    }

    private DdcServiceRegistration registration(Map<String, String> metadata) {
        return new DdcServiceRegistration(
                "provider-1",
                new DdcServiceKey(
                        "test",
                        "default",
                        DdcServiceKind.RPC_PROVIDER,
                        "egon.rpc.test.Echo",
                        "default",
                        "1.0.0",
                        "grpc"
                ),
                "127.0.0.1",
                19090,
                false,
                metadata,
                30,
                10
        );
    }
}
