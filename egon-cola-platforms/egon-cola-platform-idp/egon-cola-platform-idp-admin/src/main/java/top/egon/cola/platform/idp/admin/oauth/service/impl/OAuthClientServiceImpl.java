package top.egon.cola.platform.idp.admin.oauth.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.idp.admin.oauth.domain.dto.CreateOAuthClientDTO;
import top.egon.cola.platform.idp.admin.oauth.domain.dto.UpdateOAuthClientDTO;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientEntity;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientRedirectUriEntity;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.OAuthClientVO;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientRedirectUriRepository;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientRepository;
import top.egon.cola.platform.idp.admin.oauth.service.OAuthClientService;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityClientResourceGrantEntity;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityResourceServerEntity;
import top.egon.cola.platform.idp.admin.resource.repo.IdentityClientResourceGrantRepository;
import top.egon.cola.platform.idp.admin.resource.repo.IdentityResourceServerRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

@Service
public class OAuthClientServiceImpl implements OAuthClientService {

    private final IdentityClientRepository clients;
    private final IdentityClientRedirectUriRepository redirects;
    private final IdentityResourceServerRepository resources;
    private final IdentityClientResourceGrantRepository grants;
    private final LongIdGenerator ids;
    private final Clock clock;

    @Autowired
    public OAuthClientServiceImpl(
            IdentityClientRepository clients,
            IdentityClientRedirectUriRepository redirects,
            IdentityResourceServerRepository resources,
            IdentityClientResourceGrantRepository grants,
            LongIdGenerator ids
    ) {
        this(clients, redirects, resources, grants, ids, Clock.systemUTC());
    }

    OAuthClientServiceImpl(
            IdentityClientRepository clients,
            IdentityClientRedirectUriRepository redirects,
            IdentityResourceServerRepository resources,
            IdentityClientResourceGrantRepository grants,
            LongIdGenerator ids,
            Clock clock
    ) {
        this.clients = Objects.requireNonNull(clients, "clients");
        this.redirects = Objects.requireNonNull(redirects, "redirects");
        this.resources = Objects.requireNonNull(resources, "resources");
        this.grants = Objects.requireNonNull(grants, "grants");
        this.ids = Objects.requireNonNull(ids, "ids");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Transactional(readOnly = true)
    @Override
    public List<OAuthClientVO> list() {
        return clients.findAll().stream()
                .sorted(Comparator.comparing(IdentityClientEntity::getClientId))
                .map(this::view)
                .toList();
    }

    @Transactional
    @Override
    public OAuthClientVO create(CreateOAuthClientDTO command) {
        Objects.requireNonNull(command, "command");
        if (clients.existsById(command.clientId())) {
            throw new IllegalStateException("OAuth client already exists");
        }
        List<String> redirectValues = exactValues(
                command.redirectUris(),
                "redirectUris"
        );
        List<String> audienceValues = exactValues(
                command.audiences(),
                "audiences"
        );
        if (redirectValues.isEmpty()) {
            throw new IllegalArgumentException(
                    "public client requires redirect URI"
            );
        }
        Instant now = clock.instant();
        IdentityClientEntity client = clients.save(
                IdentityClientEntity.createPublic(
                        command.clientId(),
                        command.clientName(),
                        command.accessTokenTtlSeconds(),
                        command.refreshTokenTtlSeconds(),
                        now
                )
        );
        redirectValues.forEach(value -> redirects.save(
                IdentityClientRedirectUriEntity.create(
                        ids.nextId(),
                        client.getClientId(),
                        value,
                        now
                )
        ));
        audienceValues.forEach(value -> saveUserGrant(
                client.getClientId(),
                resource(value),
                now
        ));
        return view(client, redirectValues, audienceValues);
    }

    @Transactional
    @Override
    public OAuthClientVO update(
            String clientId,
            UpdateOAuthClientDTO command
    ) {
        Objects.requireNonNull(command, "command");
        IdentityClientEntity client = client(clientId);
        client.update(
                command.clientName(),
                command.status(),
                command.accessTokenTtlSeconds(),
                command.refreshTokenTtlSeconds(),
                command.expectedVersion(),
                clock.instant()
        );
        clients.save(client);
        return view(client);
    }

    @Transactional
    @Override
    public OAuthClientVO putRedirectUri(String clientId, String redirectUri) {
        IdentityClientEntity client = client(clientId);
        if (!redirects.existsByClientIdAndRedirectUri(
                client.getClientId(),
                redirectUri
        )) {
            redirects.save(IdentityClientRedirectUriEntity.create(
                    ids.nextId(),
                    client.getClientId(),
                    redirectUri,
                    clock.instant()
            ));
        }
        return view(client);
    }

    @Transactional
    @Override
    public OAuthClientVO deleteRedirectUri(
            String clientId,
            String redirectUri
    ) {
        IdentityClientEntity client = client(clientId);
        redirects.deleteByClientIdAndRedirectUri(
                client.getClientId(),
                exact(redirectUri, "redirectUri")
        );
        return view(client);
    }

    @Transactional
    @Override
    public OAuthClientVO putAudience(String clientId, String audience) {
        IdentityClientEntity client = client(clientId);
        String exactAudience = exact(audience, "audience");
        IdentityResourceServerEntity resource = resource(exactAudience);
        if (!grants.existsByClientIdAndResourceServerIdAndGrantType(
                client.getClientId(),
                resource.getResourceServerId(),
                IdentityClientResourceGrantEntity.GrantType.USER_DELEGATION
        )) {
            saveUserGrant(client.getClientId(), resource, clock.instant());
        }
        return view(client);
    }

    @Transactional
    @Override
    public OAuthClientVO deleteAudience(String clientId, String audience) {
        IdentityClientEntity client = client(clientId);
        IdentityResourceServerEntity resource = resource(
                exact(audience, "audience")
        );
        grants.deleteByClientIdAndResourceServerIdAndGrantType(
                client.getClientId(),
                resource.getResourceServerId(),
                IdentityClientResourceGrantEntity.GrantType.USER_DELEGATION
        );
        return view(client);
    }

    private OAuthClientVO view(IdentityClientEntity client) {
        return view(
                client,
                redirects.findByClientId(client.getClientId()).stream()
                        .map(IdentityClientRedirectUriEntity::getRedirectUri)
                        .sorted()
                        .toList(),
                grants.findByClientIdAndGrantTypeAndStatus(
                                client.getClientId(),
                                IdentityClientResourceGrantEntity.GrantType
                                        .USER_DELEGATION,
                                IdentityClientResourceGrantEntity.Status.ACTIVE
                        ).stream()
                        .map(IdentityClientResourceGrantEntity
                                ::getResourceServerId)
                        .map(this::resourceById)
                        .map(IdentityResourceServerEntity::getResourceUri)
                        .sorted()
                        .toList()
        );
    }

    private static OAuthClientVO view(
            IdentityClientEntity client,
            List<String> redirectUris,
            List<String> audiences
    ) {
        return new OAuthClientVO(
                client.getClientId(),
                client.getClientName(),
                client.getClientType().name(),
                client.getStatus().name(),
                client.isPkceRequired(),
                client.getAccessTokenTtlSeconds(),
                client.getRefreshTokenTtlSeconds(),
                List.copyOf(redirectUris),
                List.copyOf(audiences),
                client.getVersion(),
                client.getCreatedAt(),
                client.getUpdatedAt()
        );
    }

    private IdentityClientEntity client(String clientId) {
        return clients.findById(exact(clientId, "clientId"))
                .orElseThrow(() -> new IllegalArgumentException(
                        "OAuth client was not found"
                ));
    }

    private IdentityResourceServerEntity resource(String resourceUri) {
        return resources.findByResourceUri(exact(resourceUri, "audience"))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Resource Server was not found"
                ));
    }

    private IdentityResourceServerEntity resourceById(
            String resourceServerId
    ) {
        return resources.findByResourceServerId(resourceServerId)
                .orElseThrow(() -> new IllegalStateException(
                        "Resource Grant references a missing Resource Server"
                ));
    }

    private void saveUserGrant(
            String clientId,
            IdentityResourceServerEntity resource,
            Instant now
    ) {
        grants.save(IdentityClientResourceGrantEntity.userDelegation(
                ids.nextId(),
                clientId,
                resource.getResourceServerId(),
                now
        ));
    }

    private static List<String> exactValues(
            List<String> values,
            String field
    ) {
        Objects.requireNonNull(values, field);
        LinkedHashSet<String> result = new LinkedHashSet<>();
        values.forEach(value -> result.add(exact(value, field)));
        return List.copyOf(result);
    }

    private static String exact(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
