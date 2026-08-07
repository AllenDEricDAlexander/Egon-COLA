package top.egon.cola.component.gateway.test.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.admin.application.catalog.GatewayCatalogStore;
import top.egon.cola.component.gateway.admin.mcp.application.McpValidationService;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpArtifactMetadataStore;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuleContent;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeRemoteMount;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeRemoteProvider;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeServer;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTool;
import top.egon.cola.component.gateway.core.mcp.app.McpAppArtifactStore;
import top.egon.cola.component.gateway.core.mcp.remote.RemoteMcpClient;
import top.egon.cola.component.gateway.core.mcp.security.McpApprovalPort;
import top.egon.cola.component.gateway.core.mcp.security.McpAuthorizationPort;
import top.egon.cola.component.gateway.engine.mcp.McpRuntimeProperties;
import top.egon.cola.component.gateway.engine.mcp.remote.ReactorNettyRemoteMcpClient;
import top.egon.cola.component.gateway.mcp.app.McpAppSecurityValidator;
import top.egon.cola.component.gateway.mcp.prompt.StrictPromptTemplate;
import top.egon.cola.component.gateway.mcp.protocol.McpJsonRpcCodec;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;
import top.egon.cola.component.gateway.mcp.remote.McpDialectTranslator;
import top.egon.cola.component.gateway.mcp.remote.McpRemoteEndpointValidator;
import top.egon.cola.component.gateway.mcp.resource.McpResourceUriValidator;
import top.egon.cola.component.gateway.mcp.security.McpSecurityDigests;
import top.egon.cola.component.gateway.mcp.security.McpSecurityGate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Hostile-input gates that must hold before an MCP release is accepted.
 */
class McpSecurityIT {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void rejectsSsrfAndEndpointTraversalBeforeAnyNetworkCall()
            throws Exception {
        JsonNode corpus = corpus();
        for (JsonNode blocked : corpus.path("blockedEndpoints")) {
            String endpoint = blocked.asText();
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new ReactorNettyRemoteMcpClient(objectMapper)
                            .exchange(request(endpoint)),
                    endpoint
            );
        }
        for (JsonNode allowed : corpus.path("allowedEndpoints")) {
            assertDoesNotThrow(() -> McpRemoteEndpointValidator.requireSafe(
                    allowed.asText()
            ));
        }
    }

    @Test
    void rejectsDeepJsonUnsafeUrisAndOversizedRequestConfiguration()
            throws Exception {
        JsonNode corpus = corpus();
        int depth = corpus.path("jsonDepth").asInt();
        String body = "{\"jsonrpc\":\"2.0\",\"id\":1,"
                + "\"method\":\"ping\",\"params\":"
                + "{\"value\":".repeat(depth)
                + "0"
                + "}".repeat(depth)
                + "}";
        McpProtocolException deep = assertThrows(
                McpProtocolException.class,
                () -> new McpJsonRpcCodec().decode(body)
        );
        assertEquals(McpErrorCode.MCP_INVALID_REQUEST, deep.code());

        McpResourceUriValidator uris = new McpResourceUriValidator();
        for (JsonNode blocked : corpus.path("blockedResourceUris")) {
            assertThrows(
                    RuntimeException.class,
                    () -> uris.validate(blocked.asText()),
                    blocked.asText()
            );
        }
        assertEquals(
                corpus.path("maximumRequestBytes").asLong(),
                new McpRuntimeProperties().getMaximumRequestBytes()
        );
    }

    @Test
    void rejectsExternalSchemaReferencesDuringReleaseValidation()
            throws Exception {
        String schema = objectMapper.writeValueAsString(Map.of(
                "type", "object",
                "$ref", corpus().path("externalSchemaRef").asText()
        ));
        McpRuntimeTool tool = tool("LOW", schema);
        McpValidationService validator = new McpValidationService(
                mock(GatewayCatalogStore.class),
                mock(JdbcMcpArtifactMetadataStore.class),
                objectMapper
        );

        McpValidationService.ValidationReport report = validator.validate(
                remoteContent(tool)
        );

        assertFalse(report.valid());
        assertEquals(
                "GATEWAY_MCP_SCHEMA_EXTERNAL_REF_FORBIDDEN",
                report.findings().getFirst().code()
        );
    }

    @Test
    void promptInputRemainsLiteralAndCredentialsNeverCrossTheMount()
            throws Exception {
        String hostile = corpus().path("promptInjection").asText();
        String rendered = new StrictPromptTemplate().render(
                "Review: ${topic}",
                List.of("topic"),
                Map.of("topic", hostile)
        );
        assertEquals("Review: " + hostile, rendered);

        String token = corpus().path("token").asText();
        McpDialectTranslator translator = new McpDialectTranslator();
        McpDialectTranslator.OutboundCall outbound = translator.outbound(
                McpProtocolDialect.STABLE_2025_11_25,
                McpProtocolDialect.RC_2026_07_28,
                "tools/call",
                Map.of(
                        "name", "remote_echo",
                        "token", token,
                        "value", "safe"
                ),
                Map.of("authorization", token),
                Map.of(
                        "authorization", token,
                        "traceparent", "00-trace-parent-01"
                )
        );
        assertFalse(outbound.params().containsKey("token"));
        assertFalse(outbound.meta().containsKey("authorization"));
        assertFalse(outbound.headers().containsValue(token));
        assertTrue(outbound.headers().containsKey("traceparent"));

        McpProtocolException error = assertThrows(
                McpProtocolException.class,
                () -> translator.result(RemoteMcpClient.ExchangeResponse
                        .failure(
                                -32050,
                                "upstream leaked " + token,
                                Map.of(),
                                Map.of()
                        ))
        );
        assertFalse(error.getMessage().contains("secret-token-value"));
        assertTrue(error.getMessage().contains("[redacted]"));
    }

    @Test
    void appDigestCspAndNavigationContentAreImmutable() throws Exception {
        byte[] safe = "<html><body>safe</body></html>".getBytes(
                StandardCharsets.UTF_8
        );
        String safeDigest = sha256(safe);
        McpAppSecurityValidator.Manifest safeManifest = manifest(
                safeDigest,
                safe.length
        );
        McpAppSecurityValidator validator = new McpAppSecurityValidator();
        assertDoesNotThrow(() -> validator.validate(
                safeManifest,
                new McpAppArtifactStore.ArtifactContent(
                        safe,
                        safeDigest,
                        safe.length
                )
        ));

        byte[] unsafe = "<script>window.open('https://evil')</script>"
                .getBytes(StandardCharsets.UTF_8);
        String unsafeDigest = sha256(unsafe);
        assertThrows(RuntimeException.class, () -> validator.validate(
                manifest(unsafeDigest, unsafe.length),
                new McpAppArtifactStore.ArtifactContent(
                        unsafe,
                        unsafeDigest,
                        unsafe.length
                )
        ));
        assertThrows(RuntimeException.class, () -> validator.validate(
                safeManifest,
                new McpAppArtifactStore.ArtifactContent(
                        safe,
                        "f".repeat(64),
                        safe.length
                )
        ));
    }

    @Test
    void approvalTokenIsBoundToArgumentsAndConsumedExactlyOnce() {
        McpRuntimeTool tool = tool("HIGH", "{\"type\":\"object\"}");
        Map<String, Object> arguments = Map.of("invoiceId", "invoice-7");
        String token = "single-use-approval";
        String tokenDigest = McpSecurityDigests.token(token);
        String argumentDigest = McpSecurityDigests.arguments(
                objectMapper,
                arguments
        );
        AtomicBoolean consumed = new AtomicBoolean();
        McpSecurityGate gate = new McpSecurityGate(
                request -> Mono.just(McpAuthorizationPort.Decision.allowed(
                        7L,
                        3L,
                        11L
                )),
                request -> {
                    if (!tokenDigest.equals(request.tokenDigest())
                            || !argumentDigest.equals(
                            request.argumentDigest())) {
                        return Mono.just(McpApprovalPort.Result.MISMATCH);
                    }
                    return Mono.just(consumed.compareAndSet(false, true)
                            ? McpApprovalPort.Result.APPROVED
                            : McpApprovalPort.Result.CONSUMED);
                },
                objectMapper
        );

        assertDoesNotThrow(() -> authorize(gate, tool, arguments, token));
        McpProtocolException replay = assertThrows(
                McpProtocolException.class,
                () -> authorize(gate, tool, arguments, token)
        );
        assertEquals(McpErrorCode.MCP_APPROVAL_CONSUMED, replay.code());
        McpProtocolException changed = assertThrows(
                McpProtocolException.class,
                () -> authorize(
                        gate,
                        tool,
                        Map.of("invoiceId", "invoice-8"),
                        token
                )
        );
        assertEquals(McpErrorCode.MCP_APPROVAL_MISMATCH, changed.code());
    }

    private RemoteMcpClient.ExchangeRequest request(String endpoint) {
        McpRuntimeRemoteProvider provider = new McpRuntimeRemoteProvider(
                "remote-security",
                "security",
                "Security corpus",
                McpProtocolDialect.STABLE_2025_11_25,
                "STREAMABLE_HTTP",
                endpoint,
                null,
                null,
                "security-v1",
                true
        );
        return new RemoteMcpClient.ExchangeRequest(
                provider,
                "security-1",
                "initialize",
                Map.of(),
                Map.of(),
                null,
                null,
                Duration.ofSeconds(1)
        );
    }

    private McpRuleContent remoteContent(McpRuntimeTool tool) {
        return new McpRuleContent(
                List.of(server()),
                List.of(tool),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(provider()),
                List.of(mount())
        );
    }

    private McpRuntimeServer server() {
        return new McpRuntimeServer(
                "server-security",
                "billing",
                "Billing",
                "Security fixture",
                "Use reviewed capabilities.",
                Set.of(McpProtocolDialect.STABLE_2025_11_25),
                "gateway-mcp",
                30L,
                true
        );
    }

    private McpRuntimeRemoteProvider provider() {
        return new McpRuntimeRemoteProvider(
                "provider-security",
                "remote-security",
                "Remote security",
                McpProtocolDialect.STABLE_2025_11_25,
                "STREAMABLE_HTTP",
                "https://remote.example/mcp",
                null,
                null,
                "security-v1",
                true
        );
    }

    private McpRuntimeRemoteMount mount() {
        return new McpRuntimeRemoteMount(
                "mount-security",
                "billing",
                "remote-security",
                "remote",
                Set.of("TOOL"),
                Map.of(),
                "REJECT",
                Set.of(),
                "security-v1",
                true
        );
    }

    private McpRuntimeTool tool(String risk, String inputSchema) {
        return new McpRuntimeTool(
                "tool-security",
                "billing",
                "pay_invoice",
                "Pay an invoice",
                "REMOTE_MCP",
                null,
                null,
                "mount-security",
                inputSchema,
                "{\"type\":\"object\"}",
                Map.of(),
                Set.of("invoice:pay"),
                risk,
                false,
                true
        );
    }

    private McpAppSecurityValidator.Manifest manifest(
            String digest,
            long size) {
        return new McpAppSecurityValidator.Manifest(
                "billing",
                "dashboard",
                "1.0.0",
                "ui://billing/dashboard/1.0.0",
                digest,
                size,
                McpAppArtifactStore.MCP_APP_MIME_TYPE,
                "default-src 'none'; script-src 'self'; "
                        + "connect-src 'none'; base-uri 'none'; "
                        + "form-action 'none'; frame-ancestors 'none'",
                Set.of("app:view"),
                Set.of()
        );
    }

    private void authorize(
            McpSecurityGate gate,
            McpRuntimeTool tool,
            Map<String, Object> arguments,
            String token) {
        Mono.from(gate.authorizeToolCall(
                tool,
                new McpSecurityGate.IdentityContext(
                        "https://idp.internal",
                        "subject-1",
                        "tenant-1",
                        "session-1",
                        "client-1",
                        "token-1",
                        2L,
                        Set.of("gateway-mcp"),
                        Instant.parse("2026-08-03T00:00:00Z"),
                        Instant.parse("2026-08-03T01:00:00Z"),
                        7L,
                        3L,
                        11L
                ),
                arguments,
                token
        )).block();
    }

    private JsonNode corpus() throws Exception {
        try (var input = getClass().getResourceAsStream(
                "/mcp/security-corpus.json"
        )) {
            return objectMapper.readTree(input);
        }
    }

    private String sha256(byte[] content) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(content)
        );
    }
}
