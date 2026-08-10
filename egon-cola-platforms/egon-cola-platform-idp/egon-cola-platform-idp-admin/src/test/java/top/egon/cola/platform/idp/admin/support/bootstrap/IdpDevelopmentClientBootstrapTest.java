package top.egon.cola.platform.idp.admin.support.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.OAuthClientVO;
import top.egon.cola.platform.idp.admin.oauth.service.OAuthClientService;

import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdpDevelopmentClientBootstrapTest {

    @Test
    void createsOnlyMissingPublicClients() throws Exception {
        OAuthClientService clients = mock(OAuthClientService.class);
        when(clients.list()).thenReturn(List.of(client("idp-admin-web")));
        IdpDevelopmentClientBootstrap bootstrap =
                new IdpDevelopmentClientBootstrap(clients);

        bootstrap.run(new DefaultApplicationArguments());

        verify(clients, never()).create(argThat(command ->
                command.clientId().equals("idp-admin-web")));
        verify(clients).create(argThat(command ->
                command.clientId().equals("mock-backend")
                        && command.redirectUris().equals(List.of(
                        "http://127.0.0.1:18161/oauth/callback"))
                        && command.audiences().equals(List.of("mock-backend"))));
    }

    @Test
    void replacesAnObsoleteRedirectUriOnAnExistingDevelopmentClient()
            throws Exception {
        OAuthClientService clients = mock(OAuthClientService.class);
        when(clients.list()).thenReturn(List.of(new OAuthClientVO(
                "ddc-admin-web", "DDC Admin Web", "PUBLIC", "ACTIVE", true,
                900, 604800,
                List.of("http://127.0.0.1:18151/oauth/callback"),
                List.of("ddc-admin-web"), 0,
                java.time.Instant.EPOCH, java.time.Instant.EPOCH
        )));
        IdpDevelopmentClientBootstrap bootstrap =
                new IdpDevelopmentClientBootstrap(clients);

        bootstrap.run(new DefaultApplicationArguments());

        verify(clients).putRedirectUri(
                "ddc-admin-web",
                "http://127.0.0.1:18152/oauth/callback"
        );
        verify(clients).deleteRedirectUri(
                "ddc-admin-web",
                "http://127.0.0.1:18151/oauth/callback"
        );
    }

    private static OAuthClientVO client(String clientId) {
        return new OAuthClientVO(
                clientId, clientId, "PUBLIC", "ACTIVE", true,
                900, 604800, List.of(), List.of(), 0,
                java.time.Instant.EPOCH, java.time.Instant.EPOCH);
    }
}
