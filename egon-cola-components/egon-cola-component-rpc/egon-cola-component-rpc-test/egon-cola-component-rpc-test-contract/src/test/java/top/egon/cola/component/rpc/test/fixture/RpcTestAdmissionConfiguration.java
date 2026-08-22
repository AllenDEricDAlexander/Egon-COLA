package top.egon.cola.component.rpc.test.fixture;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import top.egon.cola.platform.idp.starter.client.IdpServiceOAuth2Client;
import top.egon.cola.platform.idp.starter.client.IdpServiceTokenRequest;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Supplies the fixed IdP OAuth2 Client facade used by the RPC process test.
 */
@Configuration(proxyBeanMethods = false)
public class RpcTestAdmissionConfiguration {

    /**
     * Creates a client facade returning the fixed process-test SERVICE token.
     *
     * @return process-test OAuth2 Client facade
     */
    @Bean
    public IdpServiceOAuth2Client rpcTestServiceClient() {
        IdpServiceOAuth2Client client = mock(IdpServiceOAuth2Client.class);
        Instant issuedAt = Instant.now();
        when(client.authorize(any(IdpServiceTokenRequest.class)))
                .thenReturn(new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        "test-service-token",
                        issuedAt,
                        issuedAt.plusSeconds(300)
                ));
        return client;
    }
}
