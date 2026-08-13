package top.egon.cola.component.rpc.provider.registration;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentity;
import top.egon.cola.component.rpc.contract.identity.RpcServiceIdentity;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RpcProviderRegistryContractsTest {

    private static final RpcServiceIdentity SERVICE =
            new RpcServiceIdentity("echo.Service", "default", "1.0.0");

    @Test
    void validatesCompleteLeaseIdentityAndTimeline() {
        Instant now = Instant.now();

        assertThatThrownBy(() -> new RpcProviderLease(
                " ",
                "lease-1",
                now,
                now.plusSeconds(30)
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RpcProviderLease(
                "instance-1",
                "lease-1",
                now,
                now
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RpcProviderLeaseIdentity(
                SERVICE,
                "instance-1",
                " "
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RpcLeaseOperationResult(null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registrationCopiesMetadataIntoStableSortedOrder() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("z", "last");
        metadata.put("a", "first");

        RpcProviderRegistration registration = new RpcProviderRegistration(
                SERVICE,
                new RpcProcessIdentity(
                        "provider-test",
                        "test",
                        "127.0.0.1",
                        1,
                        "provider-1"
                ),
                "127.0.0.1",
                19090,
                false,
                metadata,
                30,
                10
        );
        metadata.put("b", "later mutation");

        assertThat(registration.metadata()).containsExactly(
                Map.entry("a", "first"),
                Map.entry("z", "last")
        );
        assertThatThrownBy(() -> registration.metadata().put("b", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void registrationRejectsInvalidEndpointAndLeaseTiming() {
        RpcProcessIdentity process = new RpcProcessIdentity(
                "provider-test",
                "test",
                "127.0.0.1",
                1,
                "provider-1"
        );

        assertThatThrownBy(() -> new RpcProviderRegistration(
                SERVICE,
                process,
                "0.0.0.0",
                19090,
                false,
                Map.of(),
                30,
                10
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RpcProviderRegistration(
                SERVICE,
                process,
                "127.0.0.1",
                19090,
                false,
                Map.of(),
                10,
                10
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
