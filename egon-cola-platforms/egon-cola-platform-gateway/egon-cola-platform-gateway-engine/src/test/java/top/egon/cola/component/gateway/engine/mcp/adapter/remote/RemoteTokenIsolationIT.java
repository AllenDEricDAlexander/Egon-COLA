package top.egon.cola.component.gateway.engine.mcp.adapter.remote;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeRemoteProvider;
import top.egon.cola.component.gateway.core.mcp.remote.RemoteAuthProvider;
import top.egon.cola.component.gateway.engine.mcp.adapter.remote.ReactorNettyRemoteMcpClient;
import top.egon.cola.component.gateway.engine.mcp.adapter.remote.ReferenceRemoteAuthProvider;
import top.egon.cola.component.gateway.mcp.remote.service.McpDialectTranslator;
import top.egon.cola.component.gateway.mcp.remote.service.McpRemoteClientPool;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RemoteTokenIsolationIT {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void reactorClientUsesResolvedCredentialAndNeverForwardsInboundBearer()
            throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<JsonNode> payload = new AtomicReference<>();
        DisposableServer remote = HttpServer.create()
                .host("127.0.0.1")
                .port(0)
                .handle((request, response) -> request.receive()
                        .aggregate()
                        .asString()
                        .flatMap(body -> {
                            authorization.set(request.requestHeaders().get(
                                    "authorization"
                            ));
                            try {
                                payload.set(MAPPER.readTree(body));
                            } catch (Exception failure) {
                                return Mono.error(failure);
                            }
                            return response.header(
                                            "content-type",
                                            "application/json"
                                    )
                                    .sendString(Mono.just("""
                                            {"jsonrpc":"2.0","id":1,
                                             "result":{"structuredContent":
                                             {"number":42}}}
                                            """))
                                    .then();
                        }))
                .bindNow(Duration.ofSeconds(2));
        try {
            String inbound = "Bearer inbound-user-token";
            String outbound = "Bearer remote-service-token";
            ReferenceRemoteAuthProvider auth =
                    new ReferenceRemoteAuthProvider(
                            (reference, context) -> Mono.just(
                                    ReferenceRemoteAuthProvider.Profile
                                            .secretReference(outbound)
                            ),
                            request -> Mono.error(new AssertionError(
                                    "static secret must not call OAuth"
                            ))
                    );
            McpRemoteClientPool clients = new McpRemoteClientPool(
                    provider -> new ReactorNettyRemoteMcpClient(MAPPER),
                    auth
            );
            McpRuntimeRemoteProvider provider = new McpRuntimeRemoteProvider(
                    "provider-1",
                    "fixture",
                    "Fixture",
                    McpProtocolDialect.RC_2026_07_28,
                    "STREAMABLE_HTTP",
                    "http://127.0.0.1:" + remote.port() + "/mcp",
                    "secret://remote/fixture",
                    null,
                    "fingerprint-1",
                    true
            );
            McpDialectTranslator.OutboundCall call =
                    new McpDialectTranslator().outbound(
                            McpProtocolDialect.STABLE_2025_11_25,
                            provider.dialect(),
                            "tools/call",
                            Map.of(
                                    "name", "create_issue",
                                    "arguments", Map.of("title", "test")
                            ),
                            Map.of("traceparent", "00-trace"),
                            Map.of("traceparent", "00-trace")
                    );

            var result = Mono.from(clients.exchange(
                    provider,
                    call,
                    new RemoteAuthProvider.AuthContext(
                            "user-1",
                            "tenant-1",
                            "client-1"
                    )
            )).block(Duration.ofSeconds(2));

            assertEquals(42, ((Map<?, ?>) result.result().get(
                    "structuredContent"
            )).get("number"));
            assertEquals(outbound, authorization.get());
            assertFalse(authorization.get().equals(inbound));
            assertFalse(payload.get().toString().contains(inbound));
            assertEquals("2026-07-28", payload.get()
                    .path("params")
                    .path("_meta")
                    .path("protocolVersion")
                    .asText());
        } finally {
            remote.disposeNow(Duration.ofSeconds(2));
        }
    }
}
