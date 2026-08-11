package top.egon.cola.component.gateway.engine.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.core.operation.GatewayInvocationResult;
import top.egon.cola.component.gateway.core.operation.GatewayOperationInvocation;
import top.egon.cola.component.gateway.mcp.task.McpTask;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class McpTaskOperationExecutorTest {

    @Test
    void exchangesTheDurableUserTaskForAnExactResourceServiceToken() {
        AtomicReference<GatewayOperationInvocation> captured =
                new AtomicReference<>();
        AtomicReference<String> tenant = new AtomicReference<>();
        AtomicReference<URI> resource = new AtomicReference<>();
        URI providerResource = URI.create(
                "https://api.egon.internal/identity/mcp-provider"
        );
        McpTaskOperationExecutor executor = new McpTaskOperationExecutor(
                invocation -> {
                    captured.set(invocation);
                    return Mono.just(new GatewayInvocationResult(
                            200,
                            Map.of(),
                            "{\"value\":\"done\"}".getBytes(
                                    StandardCharsets.UTF_8)
                    ));
                },
                new ObjectMapper(),
                serverCode -> providerResource,
                (tenantId, resourceUri) -> {
                    tenant.set(tenantId);
                    resource.set(resourceUri);
                    return "service-token";
                }
        );

        var outcome = Mono.from(executor.execute(task())).block();

        assertThat(outcome).isNotNull();
        assertThat(outcome.payload()).containsEntry("value", "done");
        assertThat(tenant).hasValue("tenant-b");
        assertThat(resource).hasValue(providerResource);
        assertThat(captured.get().originalBearerToken())
                .isEqualTo("Bearer service-token");
        assertThat(captured.get().callerId()).isEqualTo("user-42");
        assertThat(captured.get().call().body())
                .isEqualTo(Map.of("value", "task"));
    }

    private McpTask task() {
        Instant created = Instant.parse("2026-08-11T12:00:00Z");
        return new McpTask(
                "task-1",
                "fingerprint",
                "user-42",
                "tenant-b",
                "mock-backend",
                "local",
                "local_echo_task",
                "a".repeat(64),
                McpTask.State.WORKING,
                Map.of(
                        "operationId", "local-echo-task-operation",
                        "body", Map.of("value", "task")
                ),
                null,
                null,
                "engine-a",
                created.plusSeconds(30),
                created.plusSeconds(60),
                created.plusSeconds(300),
                1,
                3,
                1,
                created,
                created
        );
    }
}
