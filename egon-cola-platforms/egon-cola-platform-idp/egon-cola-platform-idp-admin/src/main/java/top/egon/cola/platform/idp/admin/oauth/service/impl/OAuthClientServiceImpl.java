package top.egon.cola.platform.idp.admin.oauth.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.idp.admin.oauth.domain.dto.CreateOAuthClientDTO;
import top.egon.cola.platform.idp.admin.oauth.domain.dto.UpdateOAuthClientDTO;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientAudienceEntity;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientEntity;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientRedirectUriEntity;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.OAuthClientVO;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientAudienceRepository;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientRedirectUriRepository;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientRepository;
import top.egon.cola.platform.idp.admin.oauth.service.OAuthClientService;

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
    private final IdentityClientAudienceRepository audiences;
    private final LongIdGenerator ids;
    private final Clock clock;

    @Autowired
    public OAuthClientServiceImpl(
            IdentityClientRepository clients,
            IdentityClientRedirectUriRepository redirects,
            IdentityClientAudienceRepository audiences,
            LongIdGenerator ids
    ) {
        this(clients, redirects, audiences, ids, Clock.systemUTC());
    }

    OAuthClientServiceImpl(
            IdentityClientRepository clients,
            IdentityClientRedirectUriRepository redirects,
            IdentityClientAudienceRepository audiences,
            LongIdGenerator ids,
            Clock clock
    ) {
        this.clients = Objects.requireNonNull(clients, "clients");
        this.redirects = Objects.requireNonNull(redirects, "redirects");
        this.audiences = Objects.requireNonNull(audiences, "audiences");
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
        if (redirectValues.isEmpty() || audienceValues.isEmpty()) {
            throw new IllegalArgumentException(
                    "public client requires redirect URI and audience"
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
        audienceValues.forEach(value -> audiences.save(
                IdentityClientAudienceEntity.create(
                        ids.nextId(),
                        client.getClientId(),
                        value,
                        now
                )
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
        if (!audiences.existsByClientIdAndAudience(
                client.getClientId(),
                exactAudience
        )) {
            audiences.save(IdentityClientAudienceEntity.create(
                    ids.nextId(),
                    client.getClientId(),
                    exactAudience,
                    clock.instant()
            ));
        }
        return view(client);
    }

    @Transactional
    @Override
    public OAuthClientVO deleteAudience(String clientId, String audience) {
        IdentityClientEntity client = client(clientId);
        audiences.deleteByClientIdAndAudience(
                client.getClientId(),
                exact(audience, "audience")
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
                audiences.findByClientId(client.getClientId()).stream()
                        .map(IdentityClientAudienceEntity::getAudience)
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
