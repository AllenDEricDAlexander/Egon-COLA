package top.egon.cola.component.gateway.core.security;

import org.reactivestreams.Publisher;

public interface GatewayAuthorizationProvider {

    String providerId();

    Publisher<AuthorizationDecision> authorize(
            GatewayAuthContext context);
}
