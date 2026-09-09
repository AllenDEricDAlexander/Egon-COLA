package top.egon.cola.component.yuheng.core.security;

import org.reactivestreams.Publisher;

import java.util.Set;

public interface GatewayAuthenticationProvider {

    String providerId();

    Set<String> supportedCredentialTypes();

    Publisher<AuthenticationDecision> authenticate(
            GatewayAuthContext context,
            GatewayCredential credential);
}
