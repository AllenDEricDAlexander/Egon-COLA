package top.egon.cola.platform.idp.admin.bootstrap;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import top.egon.cola.platform.idp.admin.oauth.application.OAuthClientAdminService;

import java.util.List;
import java.util.Objects;
import java.util.Set;
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
            new ClientSpec("ddc-admin-web", "DDC Admin Web", 18151),
            new ClientSpec("mock-backend", "Unified Identity Mock Backend", 18161));

    private final OAuthClientAdminService clients;

    public IdpDevelopmentClientBootstrap(OAuthClientAdminService clients) {
        this.clients = Objects.requireNonNull(clients, "clients");
    }

    @Override
    public void run(ApplicationArguments arguments) {
        Set<String> existing = clients.list().stream()
                .map(OAuthClientAdminService.ClientView::clientId)
                .collect(Collectors.toUnmodifiableSet());
        CLIENTS.stream()
                .filter(client -> !existing.contains(client.clientId()))
                .forEach(this::create);
    }

    private void create(ClientSpec client) {
        clients.create(new OAuthClientAdminService.CreateClientCommand(
                client.clientId(),
                client.clientName(),
                ACCESS_TOKEN_TTL_SECONDS,
                REFRESH_TOKEN_TTL_SECONDS,
                List.of("http://127.0.0.1:" + client.port()
                        + "/oauth/callback"),
                List.of(client.clientId())));
    }

    private record ClientSpec(String clientId, String clientName, int port) {
    }
}
