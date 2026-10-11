package top.egon.cola.platform.tianquan.shoubing.yuheng.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import top.egon.cola.component.yuheng.contract.protocol.AccessZone;
import top.egon.cola.component.yuheng.contract.protocol.GatewayProtocol;
import top.egon.cola.component.yuheng.core.context.GatewayPrincipal;
import top.egon.cola.component.yuheng.core.security.AuthenticationFailure;
import top.egon.cola.component.yuheng.core.security.GatewayAuthContext;
import top.egon.cola.component.yuheng.core.security.GatewayCredential;
import top.egon.cola.component.yuheng.core.security.SecurityDecision;
import top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditProofBO;
import top.egon.cola.platform.tianquan.shoubing.yuheng.security.domain.GatewayVerifiedIdentityBO;

class GatewayVerifiedIdentityContractTest {
    private static final Instant NOW = Instant.parse("2026-10-11T00:00:00Z");
    private static final URI RESOURCE = URI.create("https://api.example/orders");
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void trustedSlotIsOpaqueUnrestorableAndPreservesTheSixFieldPublicShape() throws Exception {
        var evidence = new GatewayVerifiedIdentityBO(proof(), Set.of("orders:read"));
        var principal = new GatewayPrincipal("identity-1", "USER", "1001", null, true, Map.of(), evidence);
        var json = mapper.readTree(mapper.writeValueAsString(principal));
        assertThat(json.size()).isEqualTo(6);
        assertThat(json.has("trustedAuthenticationEvidence")).isFalse();
        json.withObjectProperty("trustedAuthenticationEvidence").put("proof", "untrusted-proof");
        assertThat(mapper.treeToValue(json, GatewayPrincipal.class).trustedAuthenticationEvidence()).isNull();
        assertThat(principal.toString()).doesNotContain("GatewayVerifiedIdentityBO", "orders:read", "untrusted-proof", "a".repeat(64));
        assertThat(new GatewayPrincipal("identity-1", Map.of()).trustedAuthenticationEvidence()).isNull();
        assertThat(new GatewayPrincipal("identity-1", "USER", "1001", null, true, Map.of()).trustedAuthenticationEvidence()).isNull();
    }

    @Test
    void userScopesSurviveTheRealProviderAndContextWithoutBeingPackedIntoAttributes() {
        var scopes = new HashSet<String>();
        for (int index = 0; index < 64; index++) { scopes.add("orders:" + index + ":" + "r".repeat(110)); }
        var verification = new IdpGatewayJwtVerifier.Verification(identity(), AuthenticationFailure.NONE, null, scopes, proof());
        var provider = new IdpIdentityAuthenticationProvider((context, token) -> verification);
        var decision = Mono.from(provider.authenticate(context(), new GatewayCredential("bearer", "opaque-token", Map.of()))).block();
        scopes.clear();
        assertThat(decision.decision()).isEqualTo(SecurityDecision.ALLOW);
        var evidence = (GatewayVerifiedIdentityBO) decision.principal().trustedAuthenticationEvidence();
        assertThat(evidence).isNotNull();
        assertThat(evidence.verifiedScopes()).hasSize(64);
        assertThat(evidence.proof()).isSameAs(verification.proof());
        assertThat(context().withPrincipal(decision.principal()).principal().trustedAuthenticationEvidence()).isSameAs(evidence);
        assertThat(decision.principal().attributes()).doesNotContainKeys("scope", "scopes", "trustedAuthenticationEvidence", "verifiedScopes");
        assertThatThrownBy(() -> evidence.verifiedScopes().add("orders:write")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void scopesRejectOverlongNonAsciiOrMoreThan64ValuesAndNeverExposeProofInLogging() {
        assertThatThrownBy(() -> new GatewayVerifiedIdentityBO(proof(), Set.of("读取"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GatewayVerifiedIdentityBO(proof(), Set.of("r".repeat(129)))).isInstanceOf(IllegalArgumentException.class);
        var tooMany = new HashSet<String>();
        for (int index = 0; index < 65; index++) { tooMany.add("orders:" + index); }
        assertThatThrownBy(() -> new GatewayVerifiedIdentityBO(proof(), tooMany)).isInstanceOf(IllegalArgumentException.class);
        var evidence = new GatewayVerifiedIdentityBO(proof(), Set.of());
        assertThat(evidence.toString()).isEqualTo("GatewayVerifiedIdentityBO[redacted]");
    }

    private IdentityPrincipal identity() {
        return new IdentityPrincipal("identity-1", "1001", "jti-1", Set.of(RESOURCE.toASCIIString()),
                NOW.minusSeconds(1), NOW.plusSeconds(300), AuthenticationContext.password());
    }

    private TokenAuditProofBO proof() {
        return new TokenAuditProofBO(URI.create("https://idp.example/tenants/1001"), RESOURCE, "a".repeat(64), null, "b".repeat(64));
    }

    private GatewayAuthContext context() {
        return new GatewayAuthContext(AccessZone.PUBLIC, GatewayProtocol.HTTP, "orders.read", "orders", "identity",
                "/orders", "GET", Set.of("bearer"), GatewayPrincipal.anonymous(), "127.0.0.1", "trace-1", "request-1",
                NOW.plusSeconds(5), "release-1", Map.of());
    }
}
