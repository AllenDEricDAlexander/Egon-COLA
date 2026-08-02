package top.egon.cola.platform.idp.admin.oauth.infrastructure;

import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.idp.admin.oauth.domain.IdentityClientEntity;
import top.egon.cola.platform.idp.core.oauth.OAuthClient;
import top.egon.cola.platform.idp.core.port.OAuthClientStore;

import java.util.Objects;
import java.util.Optional;

public final class JpaOAuthClientStore implements OAuthClientStore {

    private final IdentityClientRepository clients;
    private final IdentityClientRedirectUriRepository redirects;
    private final IdentityClientAudienceRepository audiences;

    public JpaOAuthClientStore(
            IdentityClientRepository clients,
            IdentityClientRedirectUriRepository redirects,
            IdentityClientAudienceRepository audiences
    ) {
        this.clients = Objects.requireNonNull(clients, "clients");
        this.redirects = Objects.requireNonNull(redirects, "redirects");
        this.audiences = Objects.requireNonNull(audiences, "audiences");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OAuthClient> findById(String clientId) {
        return clients.findById(clientId).map(this::toDomain);
    }

    private OAuthClient toDomain(IdentityClientEntity entity) {
        return new OAuthClient(
                entity.getClientId(),
                OAuthClient.ClientType.valueOf(
                        entity.getClientType().name()
                ),
                OAuthClient.Status.valueOf(entity.getStatus().name()),
                entity.isPkceRequired(),
                redirects.findByClientId(entity.getClientId()).stream()
                        .map(value -> value.getRedirectUri())
                        .toList(),
                audiences.findByClientId(entity.getClientId()).stream()
                        .map(value -> value.getAudience())
                        .toList()
        );
    }
}
