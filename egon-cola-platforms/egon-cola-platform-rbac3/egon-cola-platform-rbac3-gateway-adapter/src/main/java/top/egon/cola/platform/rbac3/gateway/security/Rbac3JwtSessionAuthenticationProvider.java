package top.egon.cola.platform.rbac3.gateway.security;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import top.egon.cola.component.gateway.core.context.GatewayPrincipal;
import top.egon.cola.component.gateway.core.security.AuthenticationDecision;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.component.gateway.core.security.GatewayAuthenticationProvider;
import top.egon.cola.component.gateway.core.security.GatewayCredential;
import top.egon.cola.platform.rbac3.contract.auth.Rbac3TokenClaims;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Verifies the JWT and all three runtime versions away from Reactor event loops.
 */
public final class Rbac3JwtSessionAuthenticationProvider
        implements GatewayAuthenticationProvider {

    public static final String PROVIDER_ID = "rbac3-jwt-session";

    private final TokenVerifier tokenVerifier;
    private final SessionVerifier sessionVerifier;

    public Rbac3JwtSessionAuthenticationProvider(
            TokenVerifier tokenVerifier,
            SessionVerifier sessionVerifier
    ) {
        this.tokenVerifier = Objects.requireNonNull(tokenVerifier, "tokenVerifier");
        this.sessionVerifier = Objects.requireNonNull(sessionVerifier, "sessionVerifier");
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
            return Mono.just(AuthenticationDecision.deny("RBAC3_CREDENTIAL_TYPE_INVALID"));
        }
        return Mono.fromCallable(() -> {
                    Rbac3TokenClaims claims = tokenVerifier.verify(
                            credential.tokenReference());
                    sessionVerifier.verify(claims);
                    return AuthenticationDecision.allow(new GatewayPrincipal(
                            claims.sub(), "USER", claims.tid(), null, true,
                            Map.of(
                                    "rbac3.session-id", claims.sid(),
                                    "rbac3.auth-version", Long.toString(claims.av()),
                                    "rbac3.session-version", Long.toString(claims.sv()),
                                    "rbac3.policy-version", Long.toString(claims.pv())
                            )));
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorReturn(AuthenticationDecision.deny("RBAC3_AUTHENTICATION_FAILED"));
    }

    @FunctionalInterface
    public interface TokenVerifier {
        Rbac3TokenClaims verify(String token);
    }

    @FunctionalInterface
    public interface SessionVerifier {
        void verify(Rbac3TokenClaims claims);
    }
}
