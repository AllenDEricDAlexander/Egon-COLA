package top.egon.cola.component.gateway.core.security;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
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

    /**
     * Checks the IdP-owned online state after a USER access token is authenticated.
     * Providers that only support recovery remain compatible through the active default.
     */
    default Publisher<GatewayCredentialOnlineStateResult> validateAuthenticated(
            GatewayAuthContext context,
            GatewayExchange exchange) {
        return subscriber -> subscriber.onSubscribe(new Subscription() {
            private boolean completed;

            @Override
            public void request(long count) {
                if (completed || count <= 0) {
                    return;
                }
                completed = true;
                subscriber.onNext(GatewayCredentialOnlineStateResult.active());
                subscriber.onComplete();
            }

            @Override
            public void cancel() {
                completed = true;
            }
        });
    }
}
