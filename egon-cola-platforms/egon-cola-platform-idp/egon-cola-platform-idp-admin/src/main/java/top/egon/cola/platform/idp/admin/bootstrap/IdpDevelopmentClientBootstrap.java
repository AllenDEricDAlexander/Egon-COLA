package top.egon.cola.platform.idp.admin.bootstrap;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import top.egon.cola.platform.idp.admin.oauth.application.OAuthClientAdminService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Idempotently registers public OAuth clients used by the explicit local topology.
 */
@Component
@Profile("local")
@ConditionalOnProperty(
        prefix = "egon.idp.development-bootstrap",
        name = "enabled",
        havingValue = "true")
public class IdpDevelopmentClientBootstrap implements ApplicationRunner {

    private static final int ACCESS_TOKEN_TTL_SECONDS = 900;
    private static final int REFRESH_TOKEN_TTL_SECONDS = 604_800;
    private static final List<ClientSpec> CLIENTS = List.of(
            new ClientSpec("idp-admin-web", "IdP Admin Web", 18121),
            new ClientSpec("rbac3-admin-web", "RBAC3 Admin Web", 18131),
            new ClientSpec("gateway-admin-web", "Gateway Admin Web", 18141),
            new ClientSpec(
                    "ddc-admin-web",
                    "DDC Admin Web",
                    18152,
                    List.of("http://127.0.0.1:18151/oauth/callback")
            ),
            new ClientSpec("mock-backend", "Unified Identity Mock Backend", 18161));

    private final OAuthClientAdminService clients;

    public IdpDevelopmentClientBootstrap(OAuthClientAdminService clients) {
        this.clients = Objects.requireNonNull(clients, "clients");
    }

    @Override
    public void run(ApplicationArguments arguments) {
        Map<String, OAuthClientAdminService.ClientView> existing =
                clients.list().stream().collect(Collectors.toUnmodifiableMap(
                        OAuthClientAdminService.ClientView::clientId,
                        client -> client
                ));
        CLIENTS.forEach(client -> reconcile(client, existing.get(
                client.clientId()
        )));
    }

    private void reconcile(
            ClientSpec client,
            OAuthClientAdminService.ClientView existing) {
        if (existing == null) {
            create(client);
            return;
        }
        String redirectUri = redirectUri(client);
        if (!existing.redirectUris().contains(redirectUri)) {
            clients.putRedirectUri(client.clientId(), redirectUri);
        }
        client.obsoleteRedirectUris().stream()
                .filter(existing.redirectUris()::contains)
                .forEach(uri -> clients.deleteRedirectUri(
                        client.clientId(),
                        uri
                ));
    }

    private void create(ClientSpec client) {
        clients.create(new OAuthClientAdminService.CreateClientCommand(
                client.clientId(),
                client.clientName(),
                ACCESS_TOKEN_TTL_SECONDS,
                REFRESH_TOKEN_TTL_SECONDS,
                List.of(redirectUri(client)),
                List.of(client.clientId())));
    }

    private String redirectUri(ClientSpec client) {
        return "http://127.0.0.1:" + client.port() + "/oauth/callback";
    }

    private record ClientSpec(
            String clientId,
            String clientName,
            int port,
            List<String> obsoleteRedirectUris
    ) {
        private ClientSpec(String clientId, String clientName, int port) {
            this(clientId, clientName, port, List.of());
        }
    }
}
