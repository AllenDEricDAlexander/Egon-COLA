package top.egon.cola.platform.rbac3.gateway.security;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import top.egon.cola.component.gateway.core.security.AuthorizationDecision;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.component.gateway.core.security.GatewayAuthorizationProvider;

import java.util.Objects;

/**
 * Evaluates the versioned API Permission mapping and fails closed on runtime errors.
 */
public final class Rbac3PermissionAuthorizationProvider
        implements GatewayAuthorizationProvider {

    public static final String PROVIDER_ID = "rbac3-permission";

    private final DecisionSource decisionSource;

    public Rbac3PermissionAuthorizationProvider(DecisionSource decisionSource) {
        this.decisionSource = Objects.requireNonNull(decisionSource, "decisionSource");
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public Publisher<AuthorizationDecision> authorize(GatewayAuthContext context) {
        return Mono.fromCallable(() -> decisionSource.authorize(context))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorReturn(AuthorizationDecision.error(
                        "RBAC3_AUTHORIZATION_RUNTIME_UNAVAILABLE"));
    }

    @FunctionalInterface
    public interface DecisionSource {
        AuthorizationDecision authorize(GatewayAuthContext context);
    }
}
