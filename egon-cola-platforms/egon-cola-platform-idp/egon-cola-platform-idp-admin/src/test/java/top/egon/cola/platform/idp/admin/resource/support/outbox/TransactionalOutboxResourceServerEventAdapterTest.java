package top.egon.cola.platform.idp.admin.resource.support.outbox;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.OutboxReceipt;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityResourceServerEntity;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionalOutboxResourceServerEventAdapterTest {

    @Test
    void enqueuesStableDisabledEnvelopeWithoutKeyMaterial() {
        TransactionalOutbox outbox = mock(TransactionalOutbox.class);
        when(outbox.enqueue(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new OutboxReceipt("event-1", "key-1", true));
        TransactionalOutboxResourceServerEventAdapter adapter =
                new TransactionalOutboxResourceServerEventAdapter(
                        outbox,
                        Clock.fixed(
                                Instant.parse("2026-08-10T00:00:00Z"),
                                ZoneOffset.UTC
                        )
                );
        IdentityResourceServerEntity resource = resource();
        resource.disable(0L, Instant.parse("2026-08-10T00:01:00Z"));

        String eventId = adapter.enqueueDisabled(resource);

        assertThat(eventId).isNotBlank();
        ArgumentCaptor<OutboxMessage> captured =
                ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outbox).enqueue(captured.capture());
        OutboxMessage message = captured.getValue();
        assertThat(message.channel()).isEqualTo("identity-resource-runtime");
        assertThat(message.destination())
                .isEqualTo("identity.resource-server.disabled.v1");
        assertThat(message.idempotencyKey())
                .isEqualTo("permission-idp-prod:disabled:1");
        assertThat(message.payload()).isInstanceOf(Map.class);
        assertThat(message.payload().toString())
                .contains("permission-idp-prod", "permission", "idp", "prod")
                .doesNotContain("jwk", "private", "secret", "credential");
    }

    private static IdentityResourceServerEntity resource() {
        return IdentityResourceServerEntity.create(
                "row-1",
                "permission-idp-prod",
                "https://api.egon.internal/prod/permission/idp",
                "permission",
                "idp",
                "prod",
                "IdP Production",
                "idp-service",
                "idp",
                "idp:access",
                300,
                IdentityResourceServerEntity.Status.ACTIVE,
                Instant.parse("2026-08-10T00:00:00Z")
        );
    }
}
