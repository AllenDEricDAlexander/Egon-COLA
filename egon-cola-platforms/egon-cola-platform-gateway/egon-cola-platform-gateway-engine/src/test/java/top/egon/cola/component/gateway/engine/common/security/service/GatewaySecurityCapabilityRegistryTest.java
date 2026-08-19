package top.egon.cola.component.gateway.engine.common.security.service;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.core.security.AuthenticationDecision;
import top.egon.cola.component.gateway.core.security.AuthenticationMode;
import top.egon.cola.component.gateway.core.security.AuthorizationDecisionMode;
import top.egon.cola.component.gateway.core.security.CredentialExtractionResult;
import top.egon.cola.component.gateway.core.security.GatewayAuthenticationProvider;
import top.egon.cola.component.gateway.core.security.GatewayCredentialExtractor;
import top.egon.cola.component.gateway.core.security.GatewayIdentityMapper;
import top.egon.cola.component.gateway.core.security.GatewaySecurityPolicy;
import top.egon.cola.component.gateway.core.security.SecurityFailureMode;
import top.egon.cola.component.gateway.core.security.TrustedIdentity;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GatewaySecurityCapabilityRegistryTest {

    @Test
    void rejectsDuplicateCapabilityIds() {
        GatewayCredentialExtractor first = extractor("bearer");

        assertThrows(IllegalArgumentException.class, () ->
                new GatewaySecurityCapabilityRegistry(
                        List.of(first, extractor("bearer")),
                        List.of(),
                        List.of(),
                        List.of()
                )
        );
    }

    @Test
    void validatesReferencesCredentialCompatibilityAndProtocol() {
        GatewaySecurityCapabilityRegistry registry =
                new GatewaySecurityCapabilityRegistry(
                        List.of(extractor("bearer")),
                        List.of(authentication("auth", Set.of("bearer"))),
                        List.of(),
                        List.of(mapper("identity", Set.of(
                                GatewayProtocol.HTTP
                        )))
                );
        GatewaySecurityPolicy policy = policy();

        assertDoesNotThrow(() -> registry.validate(
                policy,
                Set.of(GatewayProtocol.HTTP)
        ));
        assertThrows(IllegalArgumentException.class, () -> registry.validate(
                policy,
                Set.of(GatewayProtocol.RPC)
        ));
    }

    @Test
    void rejectsMissingReferencedProvider() {
        GatewaySecurityCapabilityRegistry registry =
                new GatewaySecurityCapabilityRegistry(
                        List.of(extractor("bearer")),
                        List.of(),
                        List.of(),
                        List.of()
                );

        assertThrows(IllegalArgumentException.class, () -> registry.validate(
                policy(),
                Set.of(GatewayProtocol.HTTP)
        ));
    }

    private GatewaySecurityPolicy policy() {
        return new GatewaySecurityPolicy(
                "security",
                AuthenticationMode.REQUIRED,
                List.of("bearer"),
                List.of("auth"),
                List.of(),
                AuthorizationDecisionMode.ALL_ALLOW,
                "identity",
                Duration.ofMillis(100),
                SecurityFailureMode.FAIL_CLOSED
        );
    }

    private GatewayCredentialExtractor extractor(String id) {
        return new GatewayCredentialExtractor() {
            @Override
            public String extractorId() {
                return id;
            }

            @Override
            public String credentialType() {
                return "bearer";
            }

            @Override
            public org.reactivestreams.Publisher<
                    CredentialExtractionResult> extract(
                    top.egon.cola.component.gateway.core.exchange.GatewayExchange
                            exchange,
                    GatewaySecurityPolicy policy) {
                return Mono.just(CredentialExtractionResult.empty());
            }
        };
    }

    private GatewayAuthenticationProvider authentication(
            String id,
            Set<String> types) {
        return new GatewayAuthenticationProvider() {
            @Override
            public String providerId() {
                return id;
            }

            @Override
            public Set<String> supportedCredentialTypes() {
                return types;
            }

            @Override
            public org.reactivestreams.Publisher<AuthenticationDecision>
            authenticate(
                    top.egon.cola.component.gateway.core.security
                            .GatewayAuthContext context,
                    top.egon.cola.component.gateway.core.security
                            .GatewayCredential credential) {
                return Mono.just(AuthenticationDecision.abstain());
            }
        };
    }

    private GatewayIdentityMapper mapper(
            String id,
            Set<GatewayProtocol> protocols) {
        return new GatewayIdentityMapper() {
            @Override
            public String mapperId() {
                return id;
            }

            @Override
            public Set<GatewayProtocol> supportedProtocols() {
                return protocols;
            }

            @Override
            public TrustedIdentity map(
                    top.egon.cola.component.gateway.core.security
                            .GatewayAuthContext context) {
                return TrustedIdentity.empty();
            }
        };
    }
}
