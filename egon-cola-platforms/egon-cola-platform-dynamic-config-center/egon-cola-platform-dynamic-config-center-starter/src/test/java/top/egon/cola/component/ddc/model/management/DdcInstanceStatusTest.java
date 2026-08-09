package top.egon.cola.component.ddc.model.management;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DdcInstanceStatusTest {

    @ParameterizedTest
    @CsvSource({
            "ONLINE, ONLINE", "REGISTERED, ONLINE", "UP, ONLINE",
            "OFFLINE, OFFLINE", "EXPIRED, OFFLINE", "DOWN, OFFLINE",
            "unexpected, UNKNOWN"
    })
    void normalizesWireValues(String wire, DdcInstanceStatus expected) {
        assertThat(DdcInstanceStatus.fromWire(wire)).isEqualTo(expected);
    }

    @Test
    void treatsNullAndBlankWireValuesAsUnknown() {
        assertThat(DdcInstanceStatus.fromWire(null))
                .isEqualTo(DdcInstanceStatus.UNKNOWN);
        assertThat(DdcInstanceStatus.fromWire("   "))
                .isEqualTo(DdcInstanceStatus.UNKNOWN);
    }

    @Test
    void requiresOnlineStatusAndALeaseStrictlyAfterNow() {
        Instant now = Instant.parse("2026-07-26T00:00:00Z");

        assertThat(DdcInstanceStatus.ONLINE.isAvailable(now, now.plusSeconds(1)))
                .isTrue();
        assertThat(DdcInstanceStatus.ONLINE.isAvailable(now, now))
                .isFalse();
        assertThat(DdcInstanceStatus.ONLINE.isAvailable(now, now.minusSeconds(1)))
                .isFalse();
        assertThat(DdcInstanceStatus.ONLINE.isAvailable(now, null))
                .isFalse();
        assertThat(DdcInstanceStatus.UNKNOWN.isAvailable(now, now.plusSeconds(1)))
                .isFalse();
    }

    @Test
    void normalizesManagementInstanceStatusesWithoutChangingWireValues() {
        DdcManagementServiceInstance service =
                new DdcManagementServiceInstance(
                        "service-1", "lease-1", "127.0.0.1", 8080,
                        false, Map.of(), "REGISTERED", null, null, null
                );
        DdcManagementConfigClientInstance client =
                new DdcManagementConfigClientInstance(
                        "orders", "local", "default", "client-1", "lease-1",
                        "127.0.0.1", 8081, "CONFIG_CLIENT", "DOWN", null,
                        null, null, Map.of()
                );

        assertThat(service.status()).isEqualTo("REGISTERED");
        assertThat(service.normalizedStatus()).isEqualTo(DdcInstanceStatus.ONLINE);
        assertThat(client.status()).isEqualTo("DOWN");
        assertThat(client.normalizedStatus()).isEqualTo(DdcInstanceStatus.OFFLINE);
    }
}
