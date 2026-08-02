package top.egon.cola.component.gateway.engine.operation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayProviderServiceRef;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeOperation;
import top.egon.cola.component.gateway.core.operation.GatewayInvocationResult;
import top.egon.cola.component.gateway.core.operation.GatewayOperationInvocation;
import top.egon.cola.component.gateway.core.provider.ProviderHealthState;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderRegistryState;
import top.egon.cola.component.gateway.engine.balance.ProviderSelectionHandle;
import top.egon.cola.component.gateway.engine.rule.EngineGatewayRuleCompiler;
import top.egon.cola.component.gateway.engine.traffic.GatewayTrafficGovernance;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineGatewayOperationInvokerTest {

    @Test
    void invokesTrustedOperationDirectlyAndForwardsOnlyLocalBearer() {
        var compiled = new EngineGatewayRuleCompiler().compile(snapshot());
        AtomicBoolean released = new AtomicBoolean();
        AtomicReference<EngineGatewayOperationInvoker.PreparedRequest> prepared =
                new AtomicReference<>();
        EngineGatewayOperationInvoker invoker =
                new EngineGatewayOperationInvoker(
                        () -> compiled,
                        serviceKey -> new ProviderSelectionHandle(
                                provider(serviceKey),
                                () -> released.set(true)
                        ),
                        GatewayTrafficGovernance.noop(),
                        (provider, request, timeout) -> {
                            prepared.set(request);
                            return Mono.just(new GatewayInvocationResult(
                                    200,
                                    Map.of("content-type", List.of("application/json")),
                                    "{\"ok\":true}".getBytes(StandardCharsets.UTF_8)
                            ));
                        },
                        new ObjectMapper(),
                        Duration.ofSeconds(5),
                        1024,
                        4096
                );

        GatewayInvocationResult result = Mono.from(invoker.invoke(
                new GatewayOperationInvocation(
                        "operation-42",
                        Map.of(
                                "invoiceId", "invoice-9",
                                "providerUrl", "https://attacker.invalid"
                        ),
                        "Bearer local-jwt",
                        "user-7",
                        "127.0.0.1",
                        Map.of("traceparent", "00-trace-parent")
                )
        )).block();

        assertEquals(200, result.statusCode());
        assertEquals("GET", prepared.get().method());
        assertEquals("/invoices/invoice-9?providerUrl=https%3A%2F%2Fattacker.invalid",
                prepared.get().pathAndQuery());
        assertEquals(List.of("Bearer local-jwt"),
                prepared.get().headers().get("authorization"));
        assertFalse(prepared.get().headers().containsKey("x-provider-url"));
        assertTrue(released.get());
    }

    private GatewayRuleSnapshot snapshot() {
        GatewayRuntimeOperation operation = new GatewayRuntimeOperation(
                "operation-42",
                "billing:http:GET:/invoices/{invoiceId}",
                GatewayProtocol.HTTP,
                "GET /invoices/{invoiceId}",
                "{}",
                "{}",
                false,
                new GatewayProviderServiceRef(
                        "internal",
                        "billing",
                        "dev",
                        "default",
                        GatewayProtocol.HTTP,
                        "billing-service",
                        "default",
                        "1.0.0",
                        "HTTP"
                ),
                "STANDARD",
                Set.of(),
                Map.of("idempotent", "true"),
                false
        );
        return new GatewayRuleSnapshot(
                "version-1",
                "release-1",
                Instant.parse("2026-08-02T00:00:00Z"),
                "content-sha",
                "artifact-sha",
                new GatewayRuleContent(
                        "group-1",
                        "billing",
                        "dev",
                        "default",
                        List.of(operation),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                )
        );
    }

    private ProviderInstance provider(
            top.egon.cola.component.gateway.core.provider.ProviderServiceKey key) {
        return new ProviderInstance(
                key,
                "instance-1",
                "lease-1",
                "127.0.0.1",
                18080,
                false,
                Map.of(),
                Instant.now().plusSeconds(60),
                ProviderRegistryState.REGISTERED,
                ProviderHealthState.HEALTHY,
                ProviderHealthState.HEALTHY
        );
    }
}
