package top.egon.cola.component.gateway.mcp.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuleContent;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeRemoteMount;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeRemoteProvider;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeServer;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTool;
import top.egon.cola.component.gateway.core.mcp.remote.RemoteAuthProvider;
import top.egon.cola.component.gateway.core.mcp.remote.RemoteMcpClient;
import top.egon.cola.component.gateway.core.mcp.security.McpApprovalPort;
import top.egon.cola.component.gateway.core.mcp.security.McpAuthorizationPort;
import top.egon.cola.component.gateway.mcp.rule.CompiledMcpRules;
import top.egon.cola.component.gateway.mcp.rule.McpRuleCompiler;
import top.egon.cola.component.gateway.mcp.security.McpSecurityGate;
import top.egon.cola.component.gateway.mcp.server.McpRequestContext;
import top.egon.cola.component.gateway.mcp.tool.McpResultBinder;
import top.egon.cola.component.gateway.mcp.tool.McpToolCatalog;
import top.egon.cola.component.gateway.mcp.tool.McpToolsCallHandler;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpFederationTest {

    private static final String INBOUND = "Bearer inbound-user-token";

    private static final String OUTBOUND = "Bearer remote-service-token";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void stableClientCanCallMountedRcToolWithoutInboundTokenLeak() {
        CompiledMcpRules rules = new McpRuleCompiler().compile(rules());
        AtomicReference<RemoteMcpClient.ExchangeRequest> exchanged =
                new AtomicReference<>();
        RemoteMcpClient client = request -> {
            exchanged.set(request);
            return Mono.just(RemoteMcpClient.ExchangeResponse.success(
                    Map.of(
                            "content", List.of(Map.of(
                                    "type", "text",
                                    "text", "created"
                            )),
                            "structuredContent", Map.of("number", 42)
                    ),
                    Map.of()
            ));
        };
        RemoteAuthProvider authentication = request -> Mono.just(
                new RemoteAuthProvider.OutboundAuthentication(
                        Map.of("authorization", OUTBOUND),
                        request.provider().tlsProfileReference()
                )
        );
        McpRemoteClientPool clients = new McpRemoteClientPool(
                ignored -> client,
                authentication,
                Clock.fixed(
                        Instant.parse("2026-08-02T00:00:00Z"),
                        ZoneOffset.UTC
                ),
                Duration.ofSeconds(2),
                4,
                2,
                Duration.ofSeconds(30)
        );
        RemoteMcpToolDriver remote = new RemoteMcpToolDriver(
                () -> rules,
                clients,
                new McpNamespaceRouter(),
                new McpDialectTranslator()
        );
        McpToolsCallHandler handler = new McpToolsCallHandler(
                new McpToolCatalog(() -> rules),
                new McpResultBinder(MAPPER),
                invocation -> Mono.error(new AssertionError(
                        "remote MCP must not use the local invoker"
                )),
                allowAll(),
                MAPPER,
                null,
                () -> rules,
                remote
        );

        McpJsonRpcResponse response = Mono.from(handler.handle(
                new McpJsonRpcRequest(
                        "2.0",
                        1L,
                        "tools/call",
                        Map.of(
                                "name", "github.create_issue",
                                "arguments", Map.of("title", "Federation")
                        ),
                        Map.of("traceparent", "00-request-meta")
                ),
                context()
        )).block();

        assertEquals(42, ((Map<?, ?>) ((Map<?, ?>) response.result())
                .get("structuredContent")).get("number"));
        RemoteMcpClient.ExchangeRequest request = exchanged.get();
        assertEquals("tools/call", request.method());
        assertEquals("create_issue", request.params().get("name"));
        assertEquals("2026-07-28", request.headers().get(
                "mcp-protocol-version"
        ));
        assertEquals("tools/call", request.headers().get("mcp-method"));
        assertEquals("create_issue", request.headers().get("mcp-name"));
        assertEquals(OUTBOUND, request.headers().get("authorization"));
        assertNotEquals(INBOUND, request.headers().get("authorization"));
        assertFalse(request.headers().containsValue(INBOUND));
        assertFalse(request.params().toString().contains(INBOUND));
        assertFalse(request.meta().toString().contains(INBOUND));
    }

    @Test
    void capabilityFingerprintIsDeterministicAndDriftRequiresRepublish() {
        RemoteMcpClient client = request -> Mono.just(switch (request.method()) {
            case "initialize" -> RemoteMcpClient.ExchangeResponse.success(
                    Map.of("protocolVersion", "2026-07-28"),
                    Map.of()
            );
            case "tools/list" -> RemoteMcpClient.ExchangeResponse.success(
                    Map.of("tools", List.of(Map.of(
                            "name", "create_issue",
                            "inputSchema", Map.of("type", "object")
                    ))),
                    Map.of()
            );
            default -> RemoteMcpClient.ExchangeResponse.success(
                    Map.of(),
                    Map.of()
            );
        });
        McpRemoteClientPool clients = new McpRemoteClientPool(
                ignored -> client,
                request -> Mono.just(
                        RemoteAuthProvider.OutboundAuthentication.none()
                )
        );
        AtomicReference<McpCapabilitySynchronizer.CapabilitySnapshot> saved =
                new AtomicReference<>();
        McpCapabilitySynchronizer synchronizer =
                new McpCapabilitySynchronizer(
                        clients,
                        new McpDialectTranslator(),
                        MAPPER,
                        Clock.systemUTC(),
                        saved::set
                );

        McpRuntimeRemoteProvider configured = provider("reviewed-fingerprint");
        McpCapabilitySynchronizer.CapabilitySnapshot first = Mono.from(
                synchronizer.synchronize(configured)
        ).block();
        McpCapabilitySynchronizer.CapabilitySnapshot second = Mono.from(
                synchronizer.synchronize(configured)
        ).block();

        assertEquals(first.fingerprint(), second.fingerprint());
        assertEquals(second, saved.get());
        assertFalse(first.matchesReviewedFingerprint());
        assertThrows(
                IllegalStateException.class,
                () -> synchronizer.requireReviewed(configured, first)
        );
    }

    @Test
    void namespaceRenameRulesAreFrozenAndConflictsAreRejected() {
        McpNamespaceRouter router = new McpNamespaceRouter();
        McpRuntimeRemoteMount mount = mount(
                Map.of("create_issue", "open_issue"),
                "REJECT"
        );

        assertEquals(
                "github.open_issue",
                router.exposedName(mount, "create_issue")
        );
        assertEquals(
                "create_issue",
                router.remoteName(mount, "github.open_issue")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> router.merge(
                        Set.of("github.open_issue"),
                        mount,
                        "create_issue"
                )
        );
    }

    @Test
    void reviewedFingerprintChangeDoesNotClosePreviousInflightClient() {
        AtomicReference<TrackingRemoteClient> first = new AtomicReference<>();
        AtomicReference<TrackingRemoteClient> second = new AtomicReference<>();
        McpRemoteClientPool clients = new McpRemoteClientPool(
                provider -> {
                    TrackingRemoteClient client = new TrackingRemoteClient();
                    if (first.compareAndSet(null, client)) {
                        return client;
                    }
                    second.set(client);
                    return client;
                },
                request -> Mono.just(
                        RemoteAuthProvider.OutboundAuthentication.none()
                )
        );
        McpDialectTranslator.OutboundCall call =
                new McpDialectTranslator().outbound(
                        McpProtocolDialect.STABLE_2025_11_25,
                        McpProtocolDialect.RC_2026_07_28,
                        "tools/list",
                        Map.of(),
                        Map.of(),
                        Map.of()
                );

        Mono.from(clients.exchange(
                provider("fingerprint-1"),
                call,
                RemoteAuthProvider.AuthContext.system()
        )).block();
        Mono.from(clients.exchange(
                provider("fingerprint-2"),
                call,
                RemoteAuthProvider.AuthContext.system()
        )).block();

        assertFalse(first.get().closed.get());
        assertFalse(second.get().closed.get());
        clients.close();
        assertTrue(first.get().closed.get());
        assertTrue(second.get().closed.get());
    }

    private McpRuleContent rules() {
        return new McpRuleContent(
                List.of(server()),
                List.of(new McpRuntimeTool(
                        "tool-1",
                        "developer",
                        "github.create_issue",
                        "Create a reviewed issue",
                        "REMOTE_MCP",
                        null,
                        null,
                        "mount-1",
                        "{\"type\":\"object\"}",
                        "{\"type\":\"object\"}",
                        Map.of(),
                        Set.of("issue:create"),
                        "MEDIUM",
                        false,
                        true
                )),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(provider("fingerprint-1")),
                List.of(mount(Map.of(), "REJECT"))
        );
    }

    private McpRuntimeRemoteProvider provider(String fingerprint) {
        return new McpRuntimeRemoteProvider(
                "provider-1",
                "github",
                "GitHub MCP",
                McpProtocolDialect.RC_2026_07_28,
                "STREAMABLE_HTTP",
                "https://mcp.example.test/rpc",
                "secret://mcp/github",
                null,
                fingerprint,
                true
        );
    }

    private McpRuntimeRemoteMount mount(
            Map<String, String> rename,
            String conflictPolicy) {
        return new McpRuntimeRemoteMount(
                "mount-1",
                "developer",
                "github",
                "github",
                Set.of("TOOL", "RESOURCE", "PROMPT", "COMPLETION"),
                rename,
                conflictPolicy,
                Set.of("remote:github"),
                "fingerprint-1",
                true
        );
    }

    private McpRuntimeServer server() {
        return new McpRuntimeServer(
                "server-1",
                "developer",
                "Developer",
                "Developer tools",
                "Use reviewed remote tools.",
                Set.of(McpProtocolDialect.STABLE_2025_11_25),
                "https://resource.egon.top/gateway-mcp",
                30,
                true
        );
    }

    private McpSecurityGate allowAll() {
        return new McpSecurityGate(
                request -> Mono.just(McpAuthorizationPort.Decision.allowed(
                        1L,
                        1L,
                        1L
                )),
                request -> Mono.just(McpApprovalPort.Result.APPROVED),
                MAPPER
        );
    }

    private McpRequestContext context() {
        return new McpRequestContext(
                server(),
                McpProtocolDialect.STABLE_2025_11_25,
                "session-1",
                Map.ofEntries(
                        Map.entry("originalBearerToken", INBOUND),
                        Map.entry("callerId", "user-7"),
                        Map.entry("tenantId", "tenant-a"),
                        Map.entry("traceparent", "00-trace-parent"),
                        Map.entry("idp.issuer", "https://idp.internal"),
                        Map.entry("idp.session-id", "session-1"),
                        Map.entry("idp.client-id", "developer-web"),
                        Map.entry("idp.token-id", "token-1"),
                        Map.entry("idp.token-version", "2"),
                        Map.entry("idp.resource-uri",
                                "https://resource.egon.top/gateway-mcp"),
                        Map.entry(
                                "idp.issued-at",
                                "2026-08-02T04:59:30Z"
                        ),
                        Map.entry(
                                "idp.expires-at",
                                "2026-08-02T05:05:00Z"
                        )
                )
        );
    }

    private static final class TrackingRemoteClient
            implements RemoteMcpClient {

        private final AtomicBoolean closed = new AtomicBoolean();

        @Override
        public org.reactivestreams.Publisher<ExchangeResponse> exchange(
                ExchangeRequest request) {
            return Mono.just(ExchangeResponse.success(Map.of(), Map.of()));
        }

        @Override
        public void close() {
            closed.set(true);
        }
    }
}
