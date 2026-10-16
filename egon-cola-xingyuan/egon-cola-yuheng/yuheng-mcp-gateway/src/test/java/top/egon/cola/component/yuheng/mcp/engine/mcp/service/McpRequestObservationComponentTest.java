package top.egon.cola.component.yuheng.mcp.engine.mcp.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.yuheng.contract.protocol.AccessZone;
import top.egon.cola.component.yuheng.contract.protocol.GatewayProtocol;
import top.egon.cola.component.yuheng.core.context.GatewayPrincipal;
import top.egon.cola.component.yuheng.core.mcp.security.McpAuthorizationRequest;
import top.egon.cola.component.yuheng.core.security.GatewayAuthContext;
import top.egon.cola.component.yuheng.runtime.security.domain.GatewaySecurityException;
import top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditProofBO;
import top.egon.cola.platform.tianquan.shoubing.yuheng.security.domain.GatewayVerifiedIdentityBO;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class McpRequestObservationComponentTest {

    public static GatewayAuthContext context(McpAuthorizationRequest request) {
        var authentication = AuthenticationContext.password();
        var proof = new TokenAuditProofBO(URI.create(request.issuer()), URI.create(request.resourceUri()), null, "a".repeat(64), "b".repeat(64));
        var principal = new GatewayPrincipal(request.subjectId(), "USER", request.tenantId(), null, true, Map.of(
                "tianquan-shoubing.issuer", request.issuer(), "tianquan-shoubing.token-id", request.tokenId(),
                "tianquan-shoubing.issued-at", request.issuedAt().toString(), "tianquan-shoubing.expires-at", request.expiresAt().toString(),
                "tianquan-shoubing.audience", request.clientId(), "tianquan-shoubing-auth-acr", authentication.acr(),
                "tianquan-shoubing-auth-time", authentication.authTime().toString()), new GatewayVerifiedIdentityBO(proof, Set.of("yuheng:read")));
        return new GatewayAuthContext(AccessZone.PUBLIC, GatewayProtocol.HTTP, "yuheng.mcp.billing", null, "published-policy", "/mcp/billing", "POST", Set.of("bearer"),
                principal, "127.0.0.1", "trace-1", "request-1", Instant.now().plusSeconds(30), "release-1",
                Map.of("tianquan-shoubing.resource-uri", request.resourceUri()), Set.of("yuheng:read"));
    }

    public static McpAuthorizationRequest request() {
        var now = Instant.now();
        return new McpAuthorizationRequest("https://issuer.example/tenants/1001", "user-1", "1001", "client-1", "token-1",
                "https://resource.example/yuheng", now.minusSeconds(1), now.plusSeconds(300), Set.of("mcp:billing:resource:scopes:read"), 0, 0, 0, "opaque-user-bearer");
    }

    @Test
    void bindsOnlyOneVerifiedContextAndClosesTheBorrowedScope() {
        var observed = new McpRequestObservationComponent(); var context = context(request());
        assertTrue(observed.authorizationScope().active());
        observed.bind(context); assertSame(context, observed.verifiedContext());
        assertEquals(503, assertThrows(GatewaySecurityException.class, () -> observed.bind(context)).httpStatus());
        assertThrows(GatewaySecurityException.class, observed::requireAvailable);
        observed.close(); observed.close(); assertFalse(observed.authorizationScope().active());
        assertThrows(GatewaySecurityException.class, observed::verifiedContext);
    }

    @Test
    void aFilteredProtocolDenialCannotEraseTheRecordedDependencyFailure() {
        try (var observed = new McpRequestObservationComponent()) {
            observed.bind(context(request()));
            var cause = new IllegalStateException("controlled-dependency"); var failure = observed.unavailable(cause);
            assertSame(cause, failure.getCause());
            var protocol = new IllegalArgumentException("controlled-protocol-wrapper");
            assertSame(failure, observed.preserveSecurityFailure(protocol));
            assertSame(protocol, failure.getSuppressed()[0]);
            assertSame(failure, assertThrows(GatewaySecurityException.class, observed::requireAvailable));
        }
    }

    @Test
    void missingOrAnonymousContextCannotAuthorizeTheStrictAdapter() {
        try (var observed = new McpRequestObservationComponent()) {
            assertThrows(GatewaySecurityException.class, observed::verifiedContext);
        }
        try (var observed = new McpRequestObservationComponent()) {
            assertThrows(GatewaySecurityException.class, () -> observed.bind(null));
        }
    }
}
