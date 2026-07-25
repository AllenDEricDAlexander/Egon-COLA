package top.egon.cola.component.gateway.admin.infrastructure.messaging;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.admin.application.observability.GatewayCallEventIngestService;
import top.egon.cola.component.gateway.admin.application.observability.GatewayObservabilityStore;
import top.egon.cola.component.gateway.contract.observability.GatewayCallEventV1;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GatewayCallEventConsumerHandlerTest {

    @Test
    void projectsValidEventAndRecordsPoisonForInvalidJson()
            throws Exception {
        GatewayObservabilityStore store =
                mock(GatewayObservabilityStore.class);
        when(store.project(any(), any())).thenReturn(true);
        Clock clock = Clock.fixed(
                Instant.parse("2026-07-25T00:00:00Z"),
                ZoneOffset.UTC
        );
        GatewayCallEventConsumerHandler handler =
                new GatewayCallEventConsumerHandler(
                        new GatewayCallEventCodec(
                                JsonMapper.builder()
                                        .findAndAddModules()
                                        .build()
                        ),
                        new GatewayCallEventIngestService(
                                store,
                                clock,
                                Duration.ofDays(7)
                        ),
                        clock
                );
        byte[] payload = JsonMapper.builder()
                .findAndAddModules()
                .build()
                .writeValueAsBytes(event());

        assertEquals(
                GatewayCallEventConsumerHandler.Result.PROJECTED,
                handler.handle(new ConsumerRecord<>(
                        "calls",
                        0,
                        10,
                        "key",
                        payload
                ))
        );
        assertEquals(
                GatewayCallEventConsumerHandler.Result.POISON_RECORDED,
                handler.handle(new ConsumerRecord<>(
                        "calls",
                        0,
                        11,
                        "key",
                        new byte[]{1, 2, 3}
                ))
        );

        verify(store).project(any(), any());
        verify(store).recordFailure(any());
    }

    private GatewayCallEventV1 event() {
        return new GatewayCallEventV1(
                "v1",
                "event-1",
                100,
                101,
                new GatewayCallEventV1.Trace(
                        "0123456789abcdef0123456789abcdef",
                        "0123456789abcdef",
                        true
                ),
                new GatewayCallEventV1.Request(
                        "request-1",
                        "HTTP",
                        "PUBLIC",
                        "GET",
                        "/orders/{id}",
                        0,
                        "UNSPECIFIED"
                ),
                new GatewayCallEventV1.Routing(
                        "test",
                        "default",
                        "group-1",
                        "engine-1",
                        "release-1",
                        "operation-1",
                        "route-1",
                        Map.of("serviceKey", "order-service")
                ),
                new GatewayCallEventV1.Governance(
                        "COMPLETE",
                        "ALLOW",
                        "CLOSED",
                        "ALLOW",
                        0
                ),
                new GatewayCallEventV1.Result(
                        "SUCCESS",
                        "",
                        200,
                        "",
                        0,
                        1
                ),
                List.of()
        );
    }
}
