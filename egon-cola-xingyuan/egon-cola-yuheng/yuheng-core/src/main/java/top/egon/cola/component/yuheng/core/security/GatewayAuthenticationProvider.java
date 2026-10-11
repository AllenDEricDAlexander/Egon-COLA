package top.egon.cola.component.yuheng.core.security;

import org.reactivestreams.Publisher;

import java.util.Set;

public interface GatewayAuthenticationProvider {

    String providerId();

    Set<String> supportedCredentialTypes();

    /** Mandatory authorization dependencies are declared by the configured authentication owner. */
    default Set<String> requiredAuthorizationProviderIds() { return Set.of(); }

    Publisher<AuthenticationDecision> authenticate(
            GatewayAuthContext context,
            GatewayCredential credential);
}
