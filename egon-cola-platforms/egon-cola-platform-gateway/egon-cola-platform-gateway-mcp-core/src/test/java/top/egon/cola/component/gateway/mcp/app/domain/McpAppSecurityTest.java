package top.egon.cola.component.gateway.mcp.app.domain;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuleContent;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeApp;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeServer;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.core.mcp.app.McpAppArtifactStore;
import top.egon.cola.component.gateway.mcp.common.protocol.McpProtocolException;
import top.egon.cola.component.gateway.mcp.app.service.AppUiResourceDriver;
import top.egon.cola.component.gateway.mcp.app.service.McpAppRuntime;
import top.egon.cola.component.gateway.mcp.rule.service.McpRuleCompiler;
import top.egon.cola.component.gateway.mcp.resource.service.McpResourceCatalog;
import top.egon.cola.component.gateway.mcp.resource.service.McpResourceDriver;
import top.egon.cola.component.gateway.mcp.resource.domain.McpResourceUriValidator;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpAppSecurityTest {

    private static final byte[] SAFE_HTML = """
            <!doctype html>
            <html><head><title>Dashboard</title></head>
            <body><main>Dashboard</main></body></html>
            """.getBytes(StandardCharsets.UTF_8);

    private static final String SAFE_CSP = "default-src 'none'; "
            + "script-src 'self'; style-src 'self'; img-src 'self' data:; "
            + "connect-src https://api.example.com; base-uri 'none'; "
            + "form-action 'none'; frame-ancestors 'none'";

    @Test
    void readsOnlyVerifiedImmutableAppContent() {
        McpRuntimeApp app = app(SAFE_HTML, SAFE_CSP);
        var rules = new McpRuleCompiler().compile(new McpRuleContent(
                List.of(new McpRuntimeServer(
                        "server-id",
                        "sales",
                        "Sales",
                        null,
                        null,
                        Set.of(McpProtocolDialect.STABLE_2025_11_25),
                        "https://resource.egon.top/gateway",
                        30L,
                        true
                )), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(app), List.of(), List.of()
        ));
        McpAppArtifactStore.Reader reader = request ->
                new McpAppArtifactStore.ArtifactContent(
                        SAFE_HTML,
                        sha256(SAFE_HTML),
                        SAFE_HTML.length
                );
        McpAppRuntime runtime = new McpAppRuntime(
                () -> rules,
                reader,
                new McpAppSecurityValidator()
        );

        McpAppRuntime.AppContent content = runtime.read(
                "sales",
                "ui://sales/dashboard/1.0.0"
        );

        assertEquals(app, content.app());
        assertArrayEquals(SAFE_HTML, content.content());
        assertEquals("allow-scripts", content.responseMetadata().get(
                "sandbox"
        ));
        assertEquals("nosniff", content.responseMetadata().get(
                "x-content-type-options"
        ));
        assertEquals("disabled", content.responseMetadata().get("cookies"));

        McpResourceCatalog catalog = new McpResourceCatalog(
                () -> rules,
                new McpResourceUriValidator()
        );
        assertEquals(
                AppUiResourceDriver.DRIVER_TYPE,
                catalog.resources("sales").getFirst().driverType()
        );
        McpResourceDriver.Content resource = Mono.from(
                new AppUiResourceDriver(runtime).read(
                        catalog.resolve(
                                "sales",
                                app.resourceUri()
                        ).request(java.util.Map.of())
                )
        ).block();
        assertNotNull(resource);
        assertArrayEquals(SAFE_HTML, resource.data());
        assertEquals(SAFE_CSP, resource.metadata().get(
                "content-security-policy"
        ));
    }

    @Test
    void rejectsDigestMismatchAndHostileNavigation() {
        McpAppSecurityValidator validator = new McpAppSecurityValidator();
        McpRuntimeApp app = app(SAFE_HTML, SAFE_CSP);
        byte[] hostile = "<script>window.location='https://evil.example'</script>"
                .getBytes(StandardCharsets.UTF_8);

        assertThrows(McpProtocolException.class, () -> validator.validate(
                app,
                new McpAppArtifactStore.ArtifactContent(
                        hostile,
                        sha256(hostile),
                        hostile.length
                )
        ));
        assertThrows(McpProtocolException.class, () -> validator.validate(
                app,
                new McpAppArtifactStore.ArtifactContent(
                        SAFE_HTML,
                        "0".repeat(64),
                        SAFE_HTML.length
                )
        ));
    }

    @Test
    void rejectsMissingPermissionsWrongMimeAndForbiddenOrigins() {
        McpAppSecurityValidator validator = new McpAppSecurityValidator();

        assertThrows(McpProtocolException.class, () -> validator.validate(
                app(SAFE_HTML, SAFE_CSP, Set.of(), Set.of()),
                content(SAFE_HTML)
        ));
        assertThrows(McpProtocolException.class, () -> validator.validate(
                app(
                        SAFE_HTML,
                        SAFE_CSP.replace(
                                "https://api.example.com",
                                "https://evil.example"
                        )
                ),
                content(SAFE_HTML)
        ));
        assertThrows(McpProtocolException.class, () -> validator.validate(
                app(
                        SAFE_HTML,
                        SAFE_CSP,
                        Set.of("gateway:dashboard:read"),
                        Set.of("https://api.example.com"),
                        "text/html"
                ),
                content(SAFE_HTML)
        ));
    }

    private McpRuntimeApp app(byte[] content, String csp) {
        return app(
                content,
                csp,
                Set.of("gateway:dashboard:read"),
                Set.of("https://api.example.com")
        );
    }

    private McpRuntimeApp app(
            byte[] content,
            String csp,
            Set<String> permissions,
            Set<String> allowedOrigins) {
        return app(
                content,
                csp,
                permissions,
                allowedOrigins,
                McpAppArtifactStore.MCP_APP_MIME_TYPE
        );
    }

    private McpRuntimeApp app(
            byte[] content,
            String csp,
            Set<String> permissions,
            Set<String> allowedOrigins,
            String mimeType) {
        return new McpRuntimeApp(
                "app-id",
                "sales",
                "dashboard",
                "dashboard",
                "1.0.0",
                "ui://sales/dashboard/1.0.0",
                "artifact-id",
                "apps/dashboard/1.0.0/index.html",
                sha256(content),
                content.length,
                mimeType,
                csp,
                permissions,
                allowedOrigins,
                Set.of(),
                true
        );
    }

    private McpAppArtifactStore.ArtifactContent content(byte[] value) {
        return new McpAppArtifactStore.ArtifactContent(
                value,
                sha256(value),
                value.length
        );
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value)
            );
        } catch (java.security.NoSuchAlgorithmException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
