package top.egon.cola.platform.idp.admin.oauth.repo;

import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientEntity;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityClientResourceGrantEntity;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityResourceServerEntity;
import top.egon.cola.platform.idp.admin.resource.repo.IdentityClientResourceGrantRepository;
import top.egon.cola.platform.idp.admin.resource.repo.IdentityResourceServerRepository;
import top.egon.cola.platform.idp.core.oauth.OAuthClient;
import top.egon.cola.platform.idp.core.port.OAuthClientStore;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public class JpaOAuthClientStore implements OAuthClientStore {

    private final IdentityClientRepository clients;
    private final IdentityClientRedirectUriRepository redirects;
    private final IdentityResourceServerRepository resources;
    private final IdentityClientResourceGrantRepository grants;

    public JpaOAuthClientStore(
            IdentityClientRepository clients,
            IdentityClientRedirectUriRepository redirects,
            IdentityResourceServerRepository resources,
            IdentityClientResourceGrantRepository grants
    ) {
        this.clients = Objects.requireNonNull(clients, "clients");
        this.redirects = Objects.requireNonNull(redirects, "redirects");
        this.resources = Objects.requireNonNull(resources, "resources");
        this.grants = Objects.requireNonNull(grants, "grants");
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
                grants.findByClientIdAndGrantTypeAndStatus(
                                entity.getClientId(),
                                IdentityClientResourceGrantEntity.GrantType
                                        .USER_DELEGATION,
                                IdentityClientResourceGrantEntity.Status.ACTIVE
                        ).stream()
                        .map(value -> resources.findByResourceServerId(
                                        value.getResourceServerId()
                                ).orElseThrow(() -> new IllegalStateException(
                                        "Resource Grant references a missing "
                                                + "Resource Server"
                                )))
                        .map(IdentityResourceServerEntity::getResourceUri)
                        .toList(),
                Duration.ofSeconds(entity.getAccessTokenTtlSeconds()),
                Duration.ofSeconds(entity.getRefreshTokenTtlSeconds())
        );
    }
}
