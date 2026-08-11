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

/**
 * OAuth Client、浏览器回调和 USER_DELEGATION 管理用例实现。
 *
 * <p>Management use-case implementation for OAuth Clients, browser redirects, and
 * USER_DELEGATION grants.</p>
 *
 * <p>PUBLIC Client 保留授权码 + PKCE 所需浏览器数据；机器 CONFIDENTIAL Client 只创建主记录，
 * 后续 JWK 和 Service Grant 由 Resource Server 管理域维护。</p>
 *
 * <p>PUBLIC Clients retain browser data required by authorization code plus PKCE. Machine
 * CONFIDENTIAL Clients create only their master record; JWKs and Service Grants are subsequently
 * managed by the Resource Server administration domain.</p>
 */
@Service
public class OAuthClientServiceImpl implements OAuthClientService {

    /** OAuth Client 主记录仓储；OAuth Client master-record repository. */
    private final IdentityClientRepository clients;

    /** Client 回调地址仓储；Client redirect-URI repository. */
    private final IdentityClientRedirectUriRepository redirects;

    /** Resource Server 仓储；Resource Server repository. */
    private final IdentityResourceServerRepository resources;

    /** Client Resource Grant 仓储；Client Resource Grant repository. */
    private final IdentityClientResourceGrantRepository grants;

    /** 全局 ID 生成器；global identifier generator. */
    private final LongIdGenerator ids;

    /** UTC 业务时钟；UTC business clock. */
    private final Clock clock;

    /**
     * 创建生产 OAuth Client 管理服务。
     *
     * <p>Creates the production OAuth Client management service.</p>
     *
     * @param clients Client 主记录仓储；Client master-record repository
     * @param redirects 回调地址仓储；redirect-URI repository
     * @param resources Resource Server 仓储；Resource Server repository
     * @param grants Client Resource Grant 仓储；Client Resource Grant repository
     * @param ids 全局 ID 生成器；global identifier generator
     */
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

    /**
     * 创建可注入时钟的 OAuth Client 管理服务。
     *
     * <p>Creates an OAuth Client management service with an injectable clock.</p>
     *
     * @param clients Client 主记录仓储；Client master-record repository
     * @param redirects 回调地址仓储；redirect-URI repository
     * @param resources Resource Server 仓储；Resource Server repository
     * @param grants Client Resource Grant 仓储；Client Resource Grant repository
     * @param ids 全局 ID 生成器；global identifier generator
     * @param clock UTC 业务时钟；UTC business clock
     */
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

    /** {@inheritDoc} */
    @Transactional(readOnly = true)
    @Override
    public List<OAuthClientVO> list() {
        return clients.findAll().stream()
                .sorted(Comparator.comparing(IdentityClientEntity::getClientId))
                .map(this::view)
                .toList();
    }

    /** {@inheritDoc} */
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
        if (command.clientType() == IdentityClientEntity.ClientType.PUBLIC
                && redirectValues.isEmpty()) {
            throw new IllegalArgumentException(
                    "public client requires redirect URI"
            );
        }
        if (command.clientType()
                == IdentityClientEntity.ClientType.CONFIDENTIAL
                && (!redirectValues.isEmpty() || !audienceValues.isEmpty())) {
            throw new IllegalArgumentException(
                    "machine confidential client must not register browser values"
            );
        }
        Instant now = clock.instant();
        IdentityClientEntity client;
        if (command.clientType() == IdentityClientEntity.ClientType.PUBLIC) {
            client = IdentityClientEntity.createPublic(
                    command.clientId(),
                    command.clientName(),
                    command.accessTokenTtlSeconds(),
                    command.refreshTokenTtlSeconds(),
                    now
            );
        } else {
            client = IdentityClientEntity.createConfidential(
                    command.clientId(),
                    command.clientName(),
                    command.accessTokenTtlSeconds(),
                    command.refreshTokenTtlSeconds(),
                    now
            );
        }
        client = clients.save(client);
        String clientId = client.getClientId();
        redirectValues.forEach(value -> redirects.save(
                IdentityClientRedirectUriEntity.create(
                        ids.nextId(),
                        clientId,
                        value,
                        now
                )
        ));
        audienceValues.forEach(value -> saveUserGrant(
                clientId,
                resource(value),
                now
        ));
        return view(client, redirectValues, audienceValues);
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Transactional
    @Override
    public OAuthClientVO putRedirectUri(String clientId, String redirectUri) {
        IdentityClientEntity client = client(clientId);
        requirePublic(client);
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

    /** {@inheritDoc} */
    @Transactional
    @Override
    public OAuthClientVO deleteRedirectUri(
            String clientId,
            String redirectUri
    ) {
        IdentityClientEntity client = client(clientId);
        requirePublic(client);
        redirects.deleteByClientIdAndRedirectUri(
                client.getClientId(),
                exact(redirectUri, "redirectUri")
        );
        return view(client);
    }

    /** {@inheritDoc} */
    @Transactional
    @Override
    public OAuthClientVO putAudience(String clientId, String audience) {
        IdentityClientEntity client = client(clientId);
        requirePublic(client);
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

    /** {@inheritDoc} */
    @Transactional
    @Override
    public OAuthClientVO deleteAudience(String clientId, String audience) {
        IdentityClientEntity client = client(clientId);
        requirePublic(client);
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

    /**
     * 查询 Client 的回调与 ACTIVE USER_DELEGATION Resource 视图。
     *
     * <p>Builds a Client view with redirects and ACTIVE USER_DELEGATION Resources.</p>
     *
     * @param client Client 持久化对象；Client persistence object
     * @return Client 管理视图；Client management view
     */
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

    /**
     * 组装不可变 Client 管理视图。
     *
     * <p>Builds an immutable Client management view.</p>
     *
     * @param client Client 持久化对象；Client persistence object
     * @param redirectUris 精确回调地址；exact redirect URIs
     * @param audiences USER_DELEGATION Resource URI；USER_DELEGATION Resource URIs
     * @return Client 管理视图；Client management view
     */
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

    /**
     * 查询必需存在的 OAuth Client。
     *
     * <p>Finds a required OAuth Client.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @return Client 持久化对象；Client persistence object
     */
    private IdentityClientEntity client(String clientId) {
        return clients.findById(exact(clientId, "clientId"))
                .orElseThrow(() -> new IllegalArgumentException(
                        "OAuth client was not found"
                ));
    }

    /**
     * 限制浏览器回调和 USER_DELEGATION 管理动作只作用于 Public Client。
     *
     * <p>Restricts browser redirects and USER_DELEGATION management to Public Clients.</p>
     *
     * @param client OAuth Client 持久化对象；OAuth Client persistence object
     */
    private static void requirePublic(IdentityClientEntity client) {
        if (client.getClientType()
                != IdentityClientEntity.ClientType.PUBLIC) {
            throw new IllegalStateException(
                    "operation requires a public OAuth Client"
            );
        }
    }

    /**
     * 按精确 URI 查询 Resource Server。
     *
     * <p>Finds a Resource Server by exact URI.</p>
     *
     * @param resourceUri Resource URI；Resource URI
     * @return Resource Server 持久化对象；Resource Server persistence object
     */
    private IdentityResourceServerEntity resource(String resourceUri) {
        return resources.findByResourceUri(exact(resourceUri, "audience"))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Resource Server was not found"
                ));
    }

    /**
     * 按稳定标识查询 Grant 引用的 Resource Server。
     *
     * <p>Finds the Resource Server referenced by a grant by stable identifier.</p>
     *
     * @param resourceServerId Resource Server 标识；Resource Server identifier
     * @return Resource Server 持久化对象；Resource Server persistence object
     */
    private IdentityResourceServerEntity resourceById(
            String resourceServerId
    ) {
        return resources.findByResourceServerId(resourceServerId)
                .orElseThrow(() -> new IllegalStateException(
                        "Resource Grant references a missing Resource Server"
                ));
    }

    /**
     * 创建 Public Client 到 Resource 的 USER_DELEGATION Grant。
     *
     * <p>Creates a USER_DELEGATION grant from a Public Client to a Resource.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @param resource Resource Server；Resource Server
     * @param now 创建时间；creation instant
     */
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

    /**
     * 校验并保留输入顺序的精确文本集合。
     *
     * <p>Validates an exact text collection while retaining input order.</p>
     *
     * @param values 原始值；raw values
     * @param field 字段名；field name
     * @return 去重后的不可变值；distinct immutable values
     */
    private static List<String> exactValues(
            List<String> values,
            String field
    ) {
        Objects.requireNonNull(values, field);
        LinkedHashSet<String> result = new LinkedHashSet<>();
        values.forEach(value -> result.add(exact(value, field)));
        return List.copyOf(result);
    }

    /**
     * 校验不带首尾空白的必填文本。
     *
     * <p>Validates required text without surrounding whitespace.</p>
     *
     * @param value 待校验值；value to validate
     * @param field 字段名；field name
     * @return 已校验值；validated value
     */
    private static String exact(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
