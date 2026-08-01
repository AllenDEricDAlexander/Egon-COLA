package top.egon.cola.component.gateway.admin.rule;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayProviderServiceRef;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleActivationMode;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRequestBodyMode;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteProfile;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteTransportPolicy;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeOperation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeParameter;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimePolicy;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeRoute;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportResponseMode;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayRuleCompilerTest {

    private final GatewayRuleCanonicalizer canonicalizer =
            new GatewayRuleCanonicalizer();

    private final GatewayRuleCompiler compiler =
            new GatewayRuleCompiler(canonicalizer);

    @Test
    void canonicalContentHashIgnoresInputOrderButArtifactTracksRelease() {
        Instant generatedAt = Instant.parse("2026-07-25T00:00:00Z");
        GatewayRuleContent left = content(
                List.of(operation("b", true), operation("a", true)),
                List.of(route("b", "b"), route("a", "a"))
        );
        GatewayRuleContent right = content(
                List.of(operation("a", true), operation("b", true)),
                List.of(route("a", "a"), route("b", "b"))
        );

        CompiledGatewayRelease first = compiler.compile(
                "release-1",
                generatedAt,
                left
        );
        CompiledGatewayRelease second = compiler.compile(
                "release-2",
                generatedAt.plusSeconds(1),
                right
        );

        assertEquals(
                first.snapshot().ruleContentSha256(),
                second.snapshot().ruleContentSha256()
        );
        assertNotEquals(
                first.snapshot().artifactSha256(),
                second.snapshot().artifactSha256()
        );
        canonicalizer.verify(first.snapshot());
        assertEquals(
                GatewayRuleActivationMode.INLINE,
                first.activation().mode()
        );
    }

    @Test
    void publicRouteCannotExposeInternalOperation() {
        GatewayRuleContent invalid = content(
                List.of(operation("orders", false)),
                List.of(route("orders", "orders"))
        );

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> compiler.compile(
                        "release-1",
                        Instant.now(),
                        invalid
                )
        );

        assertTrue(failure.getMessage().contains(
                "PUBLIC route references an internal-only operation"
        ));
    }

    @Test
    void largeSnapshotUsesBoundedImmutableChunks() {
        String largeSchema = "x".repeat(
                GatewayRuleCompiler.INLINE_LIMIT_BYTES + 100
        );
        GatewayRuntimeOperation operation = new GatewayRuntimeOperation(
                "orders",
                "orders",
                GatewayProtocol.HTTP,
                "GET /orders",
                largeSchema,
                "{}",
                true,
                service(),
                "TRANSPARENT",
                Set.of(),
                Map.of(),
                false
        );

        CompiledGatewayRelease release = compiler.compile(
                "release-large",
                Instant.parse("2026-07-25T00:00:00Z"),
                content(List.of(operation), List.of(route("r", "orders")))
        );

        assertEquals(
                GatewayRuleActivationMode.CHUNKED,
                release.activation().mode()
        );
        assertTrue(release.activation().chunks().size() > 1);
        assertTrue(release.activation().chunks().stream()
                .allMatch(chunk -> chunk.size()
                        <= GatewayRuleCompiler.CHUNK_LIMIT_BYTES));
    }

    @Test
    void retryRequiresExplicitlyIdempotentOperation() {
        GatewayRuntimeOperation operation = new GatewayRuntimeOperation(
                "orders",
                "orders",
                GatewayProtocol.HTTP,
                "POST /orders",
                "{}",
                "{}",
                true,
                service(),
                "TRANSPARENT",
                Set.of("retry"),
                Map.of("idempotent", "false"),
                false
        );
        GatewayRuntimePolicy retry = new GatewayRuntimePolicy(
                "retry",
                "RETRY",
                "OPERATION",
                Map.of("maxAttempts", 2)
        );
        GatewayRuleContent invalid = new GatewayRuleContent(
                "group-1",
                "orders",
                "local",
                "default",
                List.of(operation),
                List.of(route("orders", "orders")),
                List.of(),
                List.of(retry),
                List.of(),
                List.of(),
                List.of()
        );

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> compiler.compile(
                        "release-1",
                        Instant.now(),
                        invalid
                )
        );

        assertTrue(failure.getMessage().contains(
                "Retry requires an explicitly idempotent operation"
        ));
    }

    @Test
    void compiledSnapshotPublishesOperationParameters() {
        GatewayRuntimeOperation operation = new GatewayRuntimeOperation(
                "orders",
                "orders",
                GatewayProtocol.HTTP,
                "GET /orders/{orderId}",
                "{}",
                "{}",
                List.of(new GatewayRuntimeParameter(
                        "orderId",
                        "PATH",
                        true,
                        "java.lang.String",
                        null,
                        "the order identifier"
                )),
                true,
                service(),
                "TRANSPARENT",
                Set.of(),
                Map.of(),
                false
        );

        CompiledGatewayRelease release = compiler.compile(
                "release-1",
                Instant.parse("2026-07-25T00:00:00Z"),
                content(List.of(operation), List.of(route("orders", "orders")))
        );

        assertTrue(release.snapshotJson().contains("\"orderId\""));
        assertTrue(release.snapshotJson().contains("the order identifier"));
        canonicalizer.verify(release.snapshot());
    }

    /**
     * An operation without parameters must publish the bytes it published
     * before the component existed, or every already-released snapshot fails
     * the engine's ruleContentSha256 check.
     */
    @Test
    void operationWithoutParametersPublishesItsPreviousWireShape() {
        CompiledGatewayRelease release = compiler.compile(
                "release-1",
                Instant.parse("2026-07-25T00:00:00Z"),
                content(
                        List.of(operation("orders", true)),
                        List.of(route("orders", "orders"))
                )
        );

        assertFalse(release.snapshotJson().contains("\"parameters\""));
    }

    @Test
    void typedTransportPolicyEntersTheCanonicalSnapshot() {
        GatewayRouteTransportPolicy policy = new GatewayRouteTransportPolicy(
                GatewayRouteProfile.OPENAI_HTTP,
                null,
                GatewayRequestBodyMode.STREAMING,
                GatewayTransportResponseMode.AUTO_STREAM,
                null,
                10_000L,
                null,
                null,
                null,
                null,
                null,
                false,
                false
        );
        GatewayRuntimeRoute route = new GatewayRuntimeRoute(
                "orders",
                "orders",
                "api.example.com",
                "POST",
                "/v1/**",
                Set.of(AccessZone.PUBLIC),
                0,
                true,
                policy
        );

        CompiledGatewayRelease release = compiler.compile(
                "release-1",
                Instant.parse("2026-07-25T00:00:00Z"),
                content(
                        List.of(operation("orders", true)),
                        List.of(route)
                )
        );

        assertEquals(
                policy,
                release.snapshot().content().routes().getFirst()
                        .transportPolicy()
        );
        assertTrue(release.snapshotJson().contains(
                "\"transportPolicy\":{"
                        + "\"bodyLogEnabled\":false,"
                        + "\"connectTimeoutMs\":10000,"
                        + "\"profile\":\"OPENAI_HTTP\","
                        + "\"requestBodyMode\":\"STREAMING\","
                        + "\"responseMode\":\"AUTO_STREAM\","
                        + "\"retryEnabled\":false}"
        ));
        canonicalizer.verify(release.snapshot());
    }

    @Test
    void compilerRejectsWebSocketForRpcWhenDraftValidationIsBypassed() {
        GatewayRuntimeOperation operation = operation(
                "orders",
                GatewayProtocol.RPC,
                "TRANSPARENT"
        );
        GatewayRuntimeRoute route = new GatewayRuntimeRoute(
                "orders",
                "orders",
                "api.example.com",
                "GET",
                "/v1/**",
                Set.of(AccessZone.PUBLIC),
                0,
                true,
                new GatewayRouteTransportPolicy(
                        null,
                        GatewayTransportProtocol.WEBSOCKET,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        300_000L,
                        16_777_216L,
                        false,
                        false
                )
        );

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> compiler.compile(
                        "release-1",
                        Instant.now(),
                        content(List.of(operation), List.of(route))
                )
        );

        assertTrue(failure.getMessage().contains(
                "RPC_TRANSPORT_UNSUPPORTED"
        ));
    }

    @Test
    void compilerRejectsStreamingForWrappedOperation() {
        GatewayRuntimeOperation operation = operation(
                "orders",
                GatewayProtocol.HTTP,
                "WRAPPED"
        );
        GatewayRuntimeRoute route = new GatewayRuntimeRoute(
                "orders",
                "orders",
                "api.example.com",
                "POST",
                "/v1/**",
                Set.of(AccessZone.PUBLIC),
                0,
                true,
                new GatewayRouteTransportPolicy(
                        null,
                        null,
                        GatewayRequestBodyMode.STREAMING,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        false
                )
        );

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> compiler.compile(
                        "release-1",
                        Instant.now(),
                        content(List.of(operation), List.of(route))
                )
        );

        assertTrue(failure.getMessage().contains(
                "WRAPPED_TRANSPORT_UNSUPPORTED"
        ));
    }

    @Test
    void compilerPublishesWebSocketForATransparentHttpOperation() {
        GatewayRouteTransportPolicy policy =
                new GatewayRouteTransportPolicy(
                        GatewayRouteProfile.OPENAI_HTTP,
                        GatewayTransportProtocol.WEBSOCKET,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        300_000L,
                        16_777_216L,
                        false,
                        false
                );
        GatewayRuntimeRoute route = new GatewayRuntimeRoute(
                "realtime",
                "realtime",
                "api.example.com",
                "GET",
                "/v1/realtime",
                Set.of(AccessZone.PUBLIC),
                0,
                true,
                policy
        );

        CompiledGatewayRelease release = compiler.compile(
                "release-ws",
                Instant.parse("2026-07-30T00:00:00Z"),
                content(
                        List.of(operation(
                                "realtime",
                                GatewayProtocol.HTTP,
                                "TRANSPARENT"
                        )),
                        List.of(route)
                )
        );

        assertEquals(
                policy,
                release.snapshot().content().routes().getFirst()
                        .transportPolicy()
        );
        assertTrue(release.snapshotJson().contains(
                "\"transportProtocol\":\"WEBSOCKET\""
        ));
        canonicalizer.verify(release.snapshot());
    }

    private GatewayRuleContent content(
            List<GatewayRuntimeOperation> operations,
            List<GatewayRuntimeRoute> routes) {
        return new GatewayRuleContent(
                "group-1",
                "orders",
                "local",
                "default",
                operations,
                routes,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private GatewayRuntimeOperation operation(
            String id,
            boolean externalAccessible) {
        return new GatewayRuntimeOperation(
                id,
                id,
                GatewayProtocol.HTTP,
                "GET /" + id,
                "{}",
                "{}",
                externalAccessible,
                service(),
                "TRANSPARENT",
                Set.of(),
                Map.of(),
                false
        );
    }

    private GatewayRuntimeOperation operation(
            String id,
            GatewayProtocol protocol,
            String responseMode) {
        return new GatewayRuntimeOperation(
                id,
                id,
                protocol,
                "POST /" + id,
                "{}",
                "{}",
                true,
                service(protocol),
                responseMode,
                Set.of(),
                Map.of(),
                false
        );
    }

    private GatewayRuntimeRoute route(String id, String operationId) {
        return new GatewayRuntimeRoute(
                id,
                operationId,
                "api.example.com",
                "GET",
                "/" + id,
                Set.of(AccessZone.PUBLIC),
                0,
                true
        );
    }

    private GatewayProviderServiceRef service() {
        return service(GatewayProtocol.HTTP);
    }

    private GatewayProviderServiceRef service(GatewayProtocol protocol) {
        return new GatewayProviderServiceRef(
                "test-biz",
                "test-app",
                "local",
                "default",
                protocol,
                "orders",
                "default",
                "v1",
                protocol == GatewayProtocol.HTTP ? "http" : "grpc"
        );
    }
}
