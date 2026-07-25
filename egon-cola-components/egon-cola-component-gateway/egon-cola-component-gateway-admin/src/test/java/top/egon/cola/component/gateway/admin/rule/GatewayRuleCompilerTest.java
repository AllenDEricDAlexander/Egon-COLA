package top.egon.cola.component.gateway.admin.rule;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayProviderServiceRef;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleActivationMode;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeOperation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimePolicy;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeRoute;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        return new GatewayProviderServiceRef(
                "local",
                "default",
                GatewayProtocol.HTTP,
                "orders",
                "default",
                "v1",
                "http"
        );
    }
}
