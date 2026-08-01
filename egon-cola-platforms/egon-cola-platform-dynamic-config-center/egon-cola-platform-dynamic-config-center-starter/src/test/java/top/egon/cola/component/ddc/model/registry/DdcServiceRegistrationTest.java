package top.egon.cola.component.ddc.model.registry;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;

import java.util.HexFormat;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcServiceRegistrationTest {

    @Test
    void derivesOneServiceIdForReplicasWithoutNamespace() {
        DdcServiceKey first = new DdcServiceKey(
                "retail",
                "local",
                "order",
                DdcServiceKind.HTTP_PROVIDER,
                "order-service",
                "default",
                "1.0.0",
                "http"
        );
        DdcServiceKey second = new DdcServiceKey(
                "retail",
                "local",
                "order",
                DdcServiceKind.HTTP_PROVIDER,
                "order-service",
                "default",
                "1.0.0",
                "http"
        );

        assertThat(first.canonicalValue()).isEqualTo(String.join(
                "\n",
                "ddc-service-key-v3",
                "retail",
                "local",
                "order",
                "HTTP_PROVIDER",
                "http",
                "order-service",
                "default",
                "1.0.0"
        ));
        assertThat(first.serviceId()).isEqualTo(second.serviceId());
        assertThat(HexFormat.of().parseHex(first.serviceId())).hasSize(32);
        assertThat(DdcServiceKey.parse(first.canonicalValue())).isEqualTo(first);
    }

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

    @Test
    void shouldNotChargeReservedKeysAgainstTheBusinessMetadataBudget() {
        // Adopting typed instance metadata must not shrink what a caller can register:
        // a full business allowance plus the reserved gateway.* keys has to stay valid.
        Map<String, String> metadata = new java.util.HashMap<>();
        for (int i = 0; i < 32; i++) {
            metadata.put("business.key-" + i, "value-" + i);
        }
        metadata.put("gateway.weight", "250");
        metadata.put("gateway.zone", "cn-east-1a");
        metadata.put("gateway.health-state", "UP");

        DdcServiceRegistration registration = registration(metadata);

        assertThat(registration.metadata()).hasSize(35);
    }

    @Test
    void shouldRejectMoreThanTheBusinessMetadataAllowance() {
        Map<String, String> metadata = new java.util.HashMap<>();
        for (int i = 0; i < 33; i++) {
            metadata.put("business.key-" + i, "value-" + i);
        }

        assertThatThrownBy(() -> registration(metadata))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-reserved");
    }

    private DdcServiceRegistration registration(Map<String, String> metadata) {
        return new DdcServiceRegistration(
                "provider-1",
                new DdcServiceKey(
                        "pay-biz",
                        "orders-app",
                        "dev",
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
