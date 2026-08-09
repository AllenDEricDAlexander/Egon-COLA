package top.egon.cola.component.ddc.configuration.runtime;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.model.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DdcLeaseSessionHolderTest {

    private final DdcLeaseSessionHolder holder = new DdcLeaseSessionHolder();

    @Test
    void replacesAndReturnsCurrentSession() {
        DdcLeaseSession first = session("lease-1");
        DdcLeaseSession second = session("lease-2");

        holder.replace(first);
        holder.replace(second);

        assertThat(holder.current()).contains(second);
    }

    @Test
    void oldSessionCannotClearReplacement() {
        DdcLeaseSession first = session("lease-1");
        DdcLeaseSession second = session("lease-2");
        holder.replace(first);
        holder.replace(second);

        boolean cleared = holder.compareAndClear(first);

        assertThat(cleared).isFalse();
        assertThat(holder.current()).contains(second);
    }

    @Test
    void currentSessionCanBeCleared() {
        DdcLeaseSession current = session("lease-1");
        holder.replace(current);

        assertThat(holder.compareAndClear(current)).isTrue();
        assertThat(holder.current()).isEmpty();
    }

    private DdcLeaseSession session(String leaseId) {
        Instant registeredAt = Instant.parse("2026-07-24T12:00:00Z");
        return new DdcLeaseSession(
                "instance-1",
                leaseId,
                DdcLeaseRole.CONFIG_CLIENT,
                30,
                10,
                registeredAt,
                registeredAt.plusSeconds(30)
        );
    }
}
