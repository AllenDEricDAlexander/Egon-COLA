package top.egon.cola.component.ddc.registry.state;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.common.DdcException;
import top.egon.cola.component.ddc.model.enums.DdcLeaseRole;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcActiveRegistrationIndexTest {

    @Test
    void keepsMultipleLeasesForOneRuntimeInstance() {
        DdcActiveRegistrationIndex index = new DdcActiveRegistrationIndex();
        DdcServiceKey first = service("orders-rpc");
        DdcServiceKey second = service("payments-rpc");

        index.put(first, session("runtime-1", "lease-1"));
        index.put(second, session("runtime-1", "lease-2"));

        assertThat(index.require("runtime-1", "lease-1")).isEqualTo(first);
        assertThat(index.require("runtime-1", "lease-2")).isEqualTo(second);
        index.remove("lease-1");
        assertThatThrownBy(() -> index.require("runtime-1", "lease-1"))
                .isInstanceOf(DdcException.class);
        assertThat(index.require("runtime-1", "lease-2")).isEqualTo(second);
    }

    private DdcLeaseSession session(String instanceId, String leaseId) {
        Instant now = Instant.parse("2026-08-01T00:00:00Z");
        return new DdcLeaseSession(
                instanceId,
                leaseId,
                DdcLeaseRole.RPC_PROVIDER,
                30,
                10,
                now,
                now.plusSeconds(30)
        );
    }

    private DdcServiceKey service(String serviceName) {
        return new DdcServiceKey(
                "retail",
                "local",
                "orders",
                DdcServiceKind.RPC_PROVIDER,
                serviceName,
                "default",
                "1.0.0",
                "grpc"
        );
    }
}
