package top.egon.cola.component.gateway.core.security;

import org.reactivestreams.Publisher;
import top.egon.cola.component.gateway.core.exchange.GatewayExchange;

public interface GatewayCredentialExtractor {

    String extractorId();

    String credentialType();

    Publisher<CredentialExtractionResult> extract(
            GatewayExchange exchange,
            GatewaySecurityPolicy policy);
}
