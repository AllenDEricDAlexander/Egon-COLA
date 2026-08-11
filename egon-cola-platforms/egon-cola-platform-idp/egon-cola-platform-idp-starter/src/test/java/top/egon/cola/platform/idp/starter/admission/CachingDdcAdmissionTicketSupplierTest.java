package top.egon.cola.platform.idp.starter.admission;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.model.admission.DdcAdmissionRequest;
import top.egon.cola.component.ddc.model.admission.DdcAdmissionTicket;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CachingDdcAdmissionTicketSupplierTest {

    private static final Instant NOW = Instant.parse("2026-08-10T08:00:00Z");
    private static final DdcAdmissionRequest REQUEST = new DdcAdmissionRequest(
            "rs-idp-prod",
            URI.create("https://api.example/idp"),
            "platform",
            "idp",
            "prod",
            "idp-10.0.0.8-8080"
    );

    @Test
    void cachesUntilRenewalSkewThenReplacesTheTicket() {
        MutableClock clock = new MutableClock(NOW);
        AtomicInteger calls = new AtomicInteger();
        Function<DdcAdmissionRequest, DdcAdmissionTicket> client = request ->
                ticket(
                        "ticket-" + calls.incrementAndGet(),
                        clock.instant().plusSeconds(120)
                );
        CachingDdcAdmissionTicketSupplier supplier =
                new CachingDdcAdmissionTicketSupplier(
                        client,
                        REQUEST,
                        Duration.ofSeconds(30),
                        clock
                );

        assertThat(supplier.getTicket(REQUEST).value())
                .isEqualTo("ticket-1");
        clock.advance(Duration.ofSeconds(89));
        assertThat(supplier.getTicket(REQUEST).value())
                .isEqualTo("ticket-1");
        clock.advance(Duration.ofSeconds(2));
        assertThat(supplier.getTicket(REQUEST).value())
                .isEqualTo("ticket-2");
        assertThat(calls).hasValue(2);
    }

    @Test
    void usesAnExistingTicketOnlyUntilExpiryWhenIdpIsUnavailable() {
        MutableClock clock = new MutableClock(NOW);
        AtomicInteger calls = new AtomicInteger();
        Function<DdcAdmissionRequest, DdcAdmissionTicket> client = request -> {
            if (calls.incrementAndGet() > 1) {
                throw new IllegalStateException("IdP unavailable");
            }
            return ticket("ticket-1", NOW.plusSeconds(60));
        };
        CachingDdcAdmissionTicketSupplier supplier =
                new CachingDdcAdmissionTicketSupplier(
                        client,
                        REQUEST,
                        Duration.ofSeconds(20),
                        clock
                );
        supplier.getTicket(REQUEST);
        clock.advance(Duration.ofSeconds(45));

        assertThat(supplier.getTicket(REQUEST).value())
                .isEqualTo("ticket-1");

        clock.advance(Duration.ofSeconds(16));
        assertThatThrownBy(() -> supplier.getTicket(REQUEST))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("IdP unavailable");
    }

    @Test
    void rejectsRequestsOutsideTheConfiguredResourceIdentity() {
        CachingDdcAdmissionTicketSupplier supplier =
                new CachingDdcAdmissionTicketSupplier(
                        request -> ticket("ticket", NOW.plusSeconds(120)),
                        REQUEST,
                        Duration.ofSeconds(30),
                        Clock.fixed(NOW, ZoneOffset.UTC)
                );
        DdcAdmissionRequest wrongApplication = new DdcAdmissionRequest(
                "rs-rbac3-prod",
                URI.create("https://api.example/rbac3"),
                "platform",
                "rbac3",
                "prod",
                "idp-10.0.0.8-8080"
        );

        assertThatThrownBy(() -> supplier.getTicket(wrongApplication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("admission request does not match configuration");
    }

    @Test
    void rejectsATicketWhoseReturnedTripleDiffersFromConfiguration() {
        DdcAdmissionTicket wrongTicket = new DdcAdmissionTicket(
                "wrong-ticket",
                NOW.plusSeconds(120),
                "rs-rbac3-prod",
                URI.create("https://api.example/rbac3"),
                3L,
                "platform",
                "rbac3",
                "prod",
                "idp-10.0.0.8-8080",
                "rbac3-service-2026-08"
        );
        CachingDdcAdmissionTicketSupplier supplier =
                new CachingDdcAdmissionTicketSupplier(
                        request -> wrongTicket,
                        REQUEST,
                        Duration.ofSeconds(30),
                        Clock.fixed(NOW, ZoneOffset.UTC)
                );

        assertThatThrownBy(() -> supplier.getTicket(REQUEST))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("IdP admission ticket is invalid or too short-lived");
    }

    private static DdcAdmissionTicket ticket(
            String value,
            Instant expiresAt
    ) {
        return new DdcAdmissionTicket(
                value,
                expiresAt,
                "rs-idp-prod",
                URI.create("https://api.example/idp"),
                7L,
                "platform",
                "idp",
                "prod",
                "idp-10.0.0.8-8080",
                "idp-service-2026-08"
        );
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
