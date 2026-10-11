package top.egon.cola.component.yuheng.core.security;

import org.reactivestreams.Publisher;

public interface GatewayAuthorizationProvider {

    String providerId();

    /** Mandatory providers run before the existing ordinary ANY/ALL stage. */
    default boolean mandatory() { return false; }

    Publisher<AuthorizationDecision> authorize(
            GatewayAuthContext context);
}
