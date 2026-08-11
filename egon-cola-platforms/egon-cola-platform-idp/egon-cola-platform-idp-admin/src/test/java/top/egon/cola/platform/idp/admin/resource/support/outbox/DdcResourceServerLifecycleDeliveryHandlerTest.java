package top.egon.cola.platform.idp.admin.resource.support.outbox;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.api.client.DdcManagementClient;
import top.egon.cola.component.ddc.model.management.DdcResourceAdmissionRevocationRequest;
import top.egon.cola.component.ddc.model.management.DdcResourceAdmissionRevocationResult;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryResult;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcResourceServerLifecycleDeliveryHandlerTest {

    @Test
    void deliversExactTripleRevocationAndTreatsReplayAsSuccess() {
        DdcManagementClient client = mock(DdcManagementClient.class);
        when(client.revokeResourceAdmission(any())).thenReturn(
                new DdcResourceAdmissionRevocationResult(2, 3, 2),
                new DdcResourceAdmissionRevocationResult(0, 0, 0)
        );
        DdcResourceServerLifecycleDeliveryHandler handler =
                new DdcResourceServerLifecycleDeliveryHandler(client);

        assertThat(handler.deliver(context()).kind())
                .isEqualTo(DeliveryResult.Kind.SUCCESS);
        assertThat(handler.deliver(context()).kind())
                .isEqualTo(DeliveryResult.Kind.SUCCESS);
        verify(client, org.mockito.Mockito.times(2))
                .revokeResourceAdmission(new DdcResourceAdmissionRevocationRequest(
                        "permission-idp-prod", "permission", "idp", "prod", 7L
                ));
    }

    @Test
    void transientDdcFailureRemainsRetryable() {
        DdcManagementClient client = mock(DdcManagementClient.class);
        when(client.revokeResourceAdmission(any()))
                .thenThrow(new IllegalStateException("DDC unavailable"));
        DdcResourceServerLifecycleDeliveryHandler handler =
                new DdcResourceServerLifecycleDeliveryHandler(client);

        DeliveryResult result = handler.deliver(context());

        assertThat(result.kind())
                .isEqualTo(DeliveryResult.Kind.RETRYABLE_FAILURE);
        assertThat(result.code()).isEqualTo("DDC_RESOURCE_REVOCATION_UNAVAILABLE");
    }

    private static DeliveryContext context() {
        return new DeliveryContext(
                "event-1",
                "identity-resource-runtime",
                "identity.resource-server.disabled.v1",
                """
                        {"eventId":"event-1","eventType":"identity.resource-server.disabled.v1","schemaVersion":1,"occurredAt":"2026-08-10T00:00:00Z","aggregateType":"IDENTITY_RESOURCE_SERVER","aggregateId":"permission-idp-prod","aggregateVersion":7,"payload":{"resourceServerId":"permission-idp-prod","bizCode":"permission","appCode":"idp","env":"prod","resourceVersion":7}}
                        """,
                "application/json",
                "1",
                Map.of(),
                null,
                1,
                10,
                Instant.parse("2026-08-10T00:10:00Z")
        );
    }
}
