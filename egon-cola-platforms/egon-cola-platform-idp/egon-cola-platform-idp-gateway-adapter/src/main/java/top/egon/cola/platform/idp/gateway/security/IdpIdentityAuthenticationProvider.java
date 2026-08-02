package top.egon.cola.platform.idp.gateway.security;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import top.egon.cola.component.gateway.core.context.GatewayPrincipal;
import top.egon.cola.component.gateway.core.security.AuthenticationDecision;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.component.gateway.core.security.GatewayAuthenticationProvider;
import top.egon.cola.component.gateway.core.security.GatewayCredential;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Performs identity-only authentication away from Reactor event loops.
 */
public final class IdpIdentityAuthenticationProvider
        implements GatewayAuthenticationProvider {

    public static final String PROVIDER_ID = "idp-jwt";

    private final TokenVerifier verifier;

    public IdpIdentityAuthenticationProvider(TokenVerifier verifier) {
        this.verifier = Objects.requireNonNull(verifier, "verifier");
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public Set<String> supportedCredentialTypes() {
        return Set.of("bearer");
    }

    @Override
    public Publisher<AuthenticationDecision> authenticate(
            GatewayAuthContext context,
            GatewayCredential credential
    ) {
        if (!"bearer".equalsIgnoreCase(credential.type())) {
            return Mono.just(AuthenticationDecision.deny(
                    "IDP_CREDENTIAL_TYPE_INVALID"));
        }
        return Mono.fromCallable(() -> decision(verifier.verify(
                        credential.tokenReference())))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorReturn(AuthenticationDecision.deny(
                        "IDP_AUTHENTICATION_FAILED"));
    }

    private AuthenticationDecision decision(IdentityPrincipal principal) {
        return AuthenticationDecision.allow(new GatewayPrincipal(
                principal.subject(),
                "USER",
                principal.tenantId(),
                null,
                true,
                Map.of(
                        "idp.session-id", principal.sessionId(),
                        "idp.client-id", principal.clientId(),
                        "idp.token-id", principal.tokenId(),
                        "idp.token-version", Long.toString(
                                principal.tokenVersion()),
                        "idp.audience", String.join(
                                ",",
                                new java.util.TreeSet<>(principal.audience())
                        ),
                        "idp.issued-at", principal.issuedAt().toString(),
                        "idp.expires-at", principal.expiresAt().toString()
                )));
    }

    @FunctionalInterface
    public interface TokenVerifier {

        IdentityPrincipal verify(String token);
    }
}
