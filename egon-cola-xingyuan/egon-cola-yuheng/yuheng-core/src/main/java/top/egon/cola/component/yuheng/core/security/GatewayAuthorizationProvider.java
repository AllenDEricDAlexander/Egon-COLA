package top.egon.cola.component.yuheng.core.security;

import org.reactivestreams.Publisher;

public interface GatewayAuthorizationProvider {

    String providerId();

    Publisher<AuthorizationDecision> authorize(
            GatewayAuthContext context);
}
