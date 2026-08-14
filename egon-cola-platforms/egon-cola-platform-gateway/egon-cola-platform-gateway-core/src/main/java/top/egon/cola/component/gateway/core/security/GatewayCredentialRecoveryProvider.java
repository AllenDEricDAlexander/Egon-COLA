package top.egon.cola.component.gateway.core.security;

import org.reactivestreams.Publisher;
import top.egon.cola.component.gateway.core.exchange.GatewayExchange;

/**
 * Performs one bounded credential-recovery attempt for a protected route.
 */
public interface GatewayCredentialRecoveryProvider {

    String providerId();

    Publisher<CredentialRecoveryResult> recover(
            GatewayAuthContext context,
            GatewayExchange exchange,
            AuthenticationFailure failure);
}
