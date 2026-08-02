package top.egon.cola.component.gateway.engine.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcError;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeServer;
import top.egon.cola.component.gateway.mcp.server.McpMethodDispatcher;
import top.egon.cola.component.gateway.mcp.server.McpMethodHandler;
import top.egon.cola.component.gateway.mcp.server.McpRequestContext;
import top.egon.cola.component.gateway.mcp.telemetry.McpTelemetry;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpTelemetrySecurityTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void metricsAndAuditNeverUseSecretsOrHighCardinalityIds()
            throws Exception {
        String bearer = "Bearer inbound-secret-token";
        String taskId = "task-very-high-cardinality-7";
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        AtomicReference<String> audit = new AtomicReference<>();
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-02T00:00:00Z"),
                ZoneOffset.UTC
        );
        McpTelemetry telemetry = McpTelemetry.composite(List.of(
                new MicrometerMcpTelemetry(
                        meters,
                        ObservationRegistry.create()
                ),
                new McpAuditPublisher(MAPPER, clock, audit::set)
        ));
        McpTelemetry.Scope request = telemetry.start(
                new McpTelemetry.Request(
                        "tools/call",
                        "TOOL",
                        "developer",
                        "github",
                        Map.ofEntries(
                                Map.entry("callerId", "user-7"),
                                Map.entry("tenantId", "tenant-a"),
                                Map.entry("idp.client-id", "developer-web"),
                                Map.entry("traceparent", bearer),
                                Map.entry("originalBearerToken", bearer),
                                Map.entry("taskId", taskId),
                                Map.entry(
                                        "arguments",
                                        Map.of("password", "raw-password")
                                )
                        )
                )
        );
        McpTelemetry.Child remote = request.startChild(
                McpTelemetry.ChildKind.REMOTE
        );
        remote.success();
        request.failure("MCP_FORBIDDEN");

        List<String> tagValues = meters.getMeters().stream()
                .map(Meter::getId)
                .flatMap(id -> id.getTags().stream())
                .map(tag -> tag.getValue())
                .toList();
        assertFalse(tagValues.contains(taskId));
        assertFalse(tagValues.contains(bearer));
        assertTrue(tagValues.contains("tools/call"));
        assertTrue(tagValues.contains("TOOL"));
        assertTrue(tagValues.contains("MCP_FORBIDDEN"));

        JsonNode event = MAPPER.readTree(audit.get());
        assertEquals("mcp.runtime.request", event.path("eventType").asText());
        assertEquals("developer", event.path("serverCode").asText());
        assertEquals("MCP_FORBIDDEN", event.path("status").asText());
        assertTrue(event.path("actorFingerprint").asText().length() >= 32);
        assertFalse(audit.get().contains(bearer));
        assertFalse(audit.get().contains("raw-password"));
    }

    @Test
    void auditSinkFailureNeverChangesRequestOutcome() {
        McpAuditPublisher publisher = new McpAuditPublisher(
                MAPPER,
                Clock.systemUTC(),
                ignored -> {
                    throw new IllegalStateException("audit unavailable");
                }
        );

        McpTelemetry.Scope request = publisher.start(
                new McpTelemetry.Request(
                        "ping",
                        "LIFECYCLE",
                        "developer",
                        null,
                        Map.of("callerId", "user-7")
                )
        );

        request.success();
    }

    @Test
    void dispatcherAutomaticallyCompletesTelemetryForProtocolErrors() {
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        AtomicReference<String> audit = new AtomicReference<>();
        McpTelemetry telemetry = McpTelemetry.composite(List.of(
                new MicrometerMcpTelemetry(
                        meters,
                        ObservationRegistry.create()
                ),
                new McpAuditPublisher(
                        MAPPER,
                        Clock.systemUTC(),
                        audit::set
                )
        ));
        McpMethodHandler forbidden = new McpMethodHandler() {
            @Override
            public String method() {
                return "tools/call";
            }

            @Override
            public org.reactivestreams.Publisher<McpJsonRpcResponse> handle(
                    McpJsonRpcRequest request,
                    McpRequestContext context) {
                return Mono.just(McpJsonRpcResponse.failure(
                        request.id(),
                        McpJsonRpcError.of(
                                McpErrorCode.MCP_FORBIDDEN,
                                "MCP request is forbidden"
                        )
                ));
            }
        };
        McpMethodDispatcher dispatcher = new McpMethodDispatcher(
                List.of(forbidden),
                telemetry
        );

        Mono.from(dispatcher.dispatch(
                new McpJsonRpcRequest(
                        "2.0",
                        1L,
                        "tools/call",
                        Map.of("name", "dangerous"),
                        Map.of()
                ),
                new McpRequestContext(
                        server(),
                        McpProtocolDialect.STABLE_2025_11_25,
                        "session-1",
                        Map.of(
                                "callerId", "user-7",
                                "originalBearerToken", "Bearer never-log"
                        )
                )
        )).block();

        assertEquals(1L, meters.get("gateway.mcp.requests")
                .timer()
                .count());
        assertTrue(audit.get().contains("MCP_FORBIDDEN"));
        assertFalse(audit.get().contains("never-log"));
    }

    @Test
    void telemetryStartupFailureNeverChangesRequestOutcome() {
        McpMethodHandler ping = new McpMethodHandler() {
            @Override
            public String method() {
                return "ping";
            }

            @Override
            public org.reactivestreams.Publisher<McpJsonRpcResponse> handle(
                    McpJsonRpcRequest request,
                    McpRequestContext context) {
                return Mono.just(McpJsonRpcResponse.success(
                        request.id(),
                        Map.of()
                ));
            }
        };
        McpMethodDispatcher dispatcher = new McpMethodDispatcher(
                List.of(ping),
                ignored -> {
                    throw new IllegalStateException("telemetry unavailable");
                }
        );

        McpJsonRpcResponse response = Mono.from(dispatcher.dispatch(
                new McpJsonRpcRequest(
                        "2.0",
                        1L,
                        "ping",
                        Map.of(),
                        Map.of()
                ),
                new McpRequestContext(
                        server(),
                        McpProtocolDialect.STABLE_2025_11_25,
                        "session-1",
                        Map.of()
                )
        )).block();

        assertTrue(response != null && response.error() == null);
    }

    @Test
    void configurationRejectsCredentialForwardingAndBodyLogging() {
        McpRuntimeProperties remote = new McpRuntimeProperties();
        remote.getRemote().setTokenForwarding(true);
        assertThrows(IllegalArgumentException.class, remote::validate);

        McpRuntimeProperties security = new McpRuntimeProperties();
        security.getSecurity().setTokenForwarding(true);
        assertThrows(IllegalArgumentException.class, security::validate);

        McpRuntimeProperties audit = new McpRuntimeProperties();
        audit.getAudit().setBodyLogEnabled(true);
        assertThrows(IllegalArgumentException.class, audit::validate);
    }

    @Test
    void childObservationStartsOnlyWhenOperationIsSubscribed() {
        AtomicInteger starts = new AtomicInteger();
        McpTelemetry.Scope scope = new McpTelemetry.Scope() {
            @Override
            public void remoteProvider(String providerCode) {
            }

            @Override
            public McpTelemetry.Child startChild(
                    McpTelemetry.ChildKind kind) {
                starts.incrementAndGet();
                return McpTelemetry.Child.noop();
            }

            @Override
            public void success() {
            }

            @Override
            public void failure(String errorCode) {
            }
        };

        org.reactivestreams.Publisher<String> observed =
                McpTelemetry.observeChild(
                        scope,
                        McpTelemetry.ChildKind.OPERATION,
                        Mono.just("ok")
                );

        assertEquals(0, starts.get());
        assertEquals("ok", Mono.from(observed).block());
        assertEquals(1, starts.get());
    }

    private McpRuntimeServer server() {
        return new McpRuntimeServer(
                "server-1",
                "developer",
                "Developer",
                "Developer capabilities",
                "Use reviewed tools.",
                Set.of(McpProtocolDialect.STABLE_2025_11_25),
                "gateway-mcp",
                30,
                true
        );
    }
}
