package top.egon.cola.platform.idp.admin.oauth.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.idp.admin.oauth.domain.dto.CreateOAuthClientDTO;
import top.egon.cola.platform.idp.admin.oauth.domain.dto.RotateClientSecretDTO;
import top.egon.cola.platform.idp.admin.oauth.domain.dto.UpdateOAuthClientDTO;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientEntity;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientRedirectUriEntity;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientSecretEntity;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.CreatedOAuthClientVO;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.OAuthClientVO;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.RotatedClientSecretVO;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientRedirectUriRepository;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientRepository;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientSecretRepository;
import top.egon.cola.platform.idp.admin.oauth.service.OAuthClientService;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityClientResourceGrantEntity;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityResourceServerEntity;
import top.egon.cola.platform.idp.admin.resource.repo.IdentityClientResourceGrantRepository;
import top.egon.cola.platform.idp.admin.resource.repo.IdentityResourceServerRepository;
import top.egon.cola.platform.idp.core.audit.IdentitySecurityEvent;
import top.egon.cola.platform.idp.core.audit.IdentitySecurityEventPort;
import top.egon.cola.platform.idp.core.port.PasswordHashPort;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
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
 * <p>PUBLIC Client 保留授权码 + PKCE 所需浏览器数据；机器 CONFIDENTIAL Client 同时维护
 * hash-only Secret 生命周期，Service Grant 仍由 Resource Server 管理域维护。</p>
 *
 * <p>PUBLIC Clients retain browser data required by authorization code plus PKCE. Machine
 * CONFIDENTIAL Clients manage a hash-only Secret lifecycle; Service Grants remain managed by the
 * Resource Server administration domain.</p>
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

    /** Client Secret 仓储；Client Secret repository. */
    private final IdentityClientSecretRepository secrets;

    /** 全局 ID 生成器；global identifier generator. */
    private final LongIdGenerator ids;

    /** UTC 业务时钟；UTC business clock. */
    private final Clock clock;

    /** Secret Argon2id 哈希端口；Secret Argon2id hashing port. */
    private final PasswordHashPort passwordHashes;

    /** 身份安全审计事件端口；identity security-audit event port. */
    private final IdentitySecurityEventPort securityEvents;

    /** Secret CSPRNG；cryptographically secure random source. */
    private final SecureRandom secureRandom;

    /**
     * 创建生产 OAuth Client 管理服务。
     *
     * <p>Creates the production OAuth Client management service.</p>
     *
     * @param clients Client 主记录仓储；Client master-record repository
     * @param redirects 回调地址仓储；redirect-URI repository
     * @param resources Resource Server 仓储；Resource Server repository
     * @param grants Client Resource Grant 仓储；Client Resource Grant repository
     * @param secrets Client Secret 仓储；Client Secret repository
     * @param ids 全局 ID 生成器；global identifier generator
     * @param passwordHashes Secret 哈希端口；Secret hashing port
     * @param securityEvents 安全审计事件端口；security-audit event port
     * @param secureRandom 密码学随机源；cryptographically secure random source
     */
    @Autowired
    public OAuthClientServiceImpl(
            IdentityClientRepository clients,
            IdentityClientRedirectUriRepository redirects,
            IdentityResourceServerRepository resources,
            IdentityClientResourceGrantRepository grants,
            IdentityClientSecretRepository secrets,
            LongIdGenerator ids,
            PasswordHashPort passwordHashes,
            IdentitySecurityEventPort securityEvents,
            SecureRandom secureRandom
    ) {
        this(
                clients,
                redirects,
                resources,
                grants,
                secrets,
                ids,
                Clock.systemUTC(),
                passwordHashes,
                securityEvents,
                secureRandom
        );
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
     * @param secrets Client Secret 仓储；Client Secret repository
     * @param ids 全局 ID 生成器；global identifier generator
     * @param clock UTC 业务时钟；UTC business clock
     * @param passwordHashes Secret 哈希端口；Secret hashing port
     * @param securityEvents 安全审计事件端口；security-audit event port
     * @param secureRandom 密码学随机源；cryptographically secure random source
     */
    OAuthClientServiceImpl(
            IdentityClientRepository clients,
            IdentityClientRedirectUriRepository redirects,
            IdentityResourceServerRepository resources,
            IdentityClientResourceGrantRepository grants,
            IdentityClientSecretRepository secrets,
            LongIdGenerator ids,
            Clock clock,
            PasswordHashPort passwordHashes,
            IdentitySecurityEventPort securityEvents,
            SecureRandom secureRandom
    ) {
        this.clients = Objects.requireNonNull(clients, "clients");
        this.redirects = Objects.requireNonNull(redirects, "redirects");
        this.resources = Objects.requireNonNull(resources, "resources");
        this.grants = Objects.requireNonNull(grants, "grants");
        this.secrets = Objects.requireNonNull(secrets, "secrets");
        this.ids = Objects.requireNonNull(ids, "ids");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.passwordHashes = Objects.requireNonNull(
                passwordHashes,
                "passwordHashes"
        );
        this.securityEvents = Objects.requireNonNull(
                securityEvents,
                "securityEvents"
        );
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom");
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
    public CreatedOAuthClientVO create(CreateOAuthClientDTO command) {
        return create(command, "SYSTEM");
    }

    /** {@inheritDoc} */
    @Transactional
    @Override
    public CreatedOAuthClientVO create(
            CreateOAuthClientDTO command,
            String operatorSub
    ) {
        Objects.requireNonNull(command, "command");
        String operator = operator(operatorSub);
        if (clients.existsById(command.clientId())) {
            throw new IllegalStateException("OAuth client already exists");
        }
        if (command.clientType() == IdentityClientEntity.ClientType.PUBLIC
                && command.appId() != null) {
            throw new IllegalArgumentException(
                    "public client must not register appId"
            );
        }
        if (command.clientType() == IdentityClientEntity.ClientType.CONFIDENTIAL
                && clients.findAll().stream().anyMatch(existing ->
                existing.getClientType()
                        == IdentityClientEntity.ClientType.CONFIDENTIAL
                        && command.appId().equals(existing.getAppId()))) {
            throw new IllegalStateException("OAuth appId already exists");
        }
        List<String> redirectValues = exactValues(
                command.redirectUris(),
                "redirectUris"
        );
        List<String> resourceValues = exactValues(
                command.resourceUris(),
                "resourceUris"
        );
        if (command.clientType() == IdentityClientEntity.ClientType.PUBLIC
                && redirectValues.isEmpty()) {
            throw new IllegalArgumentException(
                    "public client requires redirect URI"
            );
        }
        if (command.clientType()
                == IdentityClientEntity.ClientType.CONFIDENTIAL
                && (!redirectValues.isEmpty() || !resourceValues.isEmpty())) {
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
                    command.appId(),
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
        resourceValues.forEach(value -> saveUserGrant(
                clientId,
                resource(value),
                now
        ));
        if (client.getClientType() == IdentityClientEntity.ClientType.PUBLIC) {
            securityEvents.append(new IdentitySecurityEvent(
                    "IDENTITY_OAUTH_CLIENT_CREATED",
                    operator,
                    "CLIENT_CREATED:" + client.getClientId(),
                    "ADMIN_API",
                    now
            ));
            return new CreatedOAuthClientVO(
                    client.getClientId(),
                    client.getAppId(),
                    client.getClientName(),
                    client.getClientType().name(),
                    client.getStatus().name(),
                    null,
                    null,
                    client.getVersion(),
                    client.getCreatedAt()
            );
        }
        CreatedOAuthClientVO created = createSecret(client, now);
        securityEvents.append(new IdentitySecurityEvent(
                "IDENTITY_OAUTH_CLIENT_CREATED",
                operator,
                "CLIENT_CREATED:" + client.getClientId(),
                "ADMIN_API",
                now
        ));
        return created;
    }

    /**
     * 轮换 Confidential Client 的 active Secret。
     *
     * <p>Rotates the active Secret for a Confidential Client.</p>
     */
    @Transactional
    @Override
    public RotatedClientSecretVO rotateSecret(
            String clientId,
            RotateClientSecretDTO command
    ) {
        return rotateSecret(clientId, command, "SYSTEM");
    }

    /** {@inheritDoc} */
    @Transactional
    @Override
    public RotatedClientSecretVO rotateSecret(
            String clientId,
            RotateClientSecretDTO command,
            String operatorSub
    ) {
        Objects.requireNonNull(command, "command");
        String operator = operator(operatorSub);
        if (command.expectedVersion() < 0L) {
            throw new IllegalArgumentException("expectedVersion must not be negative");
        }
        IdentityClientEntity client = clients.findByClientIdForUpdate(
                        exact(clientId, "clientId")
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "OAuth client was not found"
                ));
        if (client.getClientType()
                != IdentityClientEntity.ClientType.CONFIDENTIAL) {
            throw new IllegalArgumentException(
                    "secret rotation requires a confidential OAuth Client"
            );
        }
        if (client.getStatus() != IdentityClientEntity.Status.ACTIVE) {
            throw new IllegalStateException(
                    "disabled OAuth client cannot rotate its secret"
            );
        }
        if (client.getVersion() != command.expectedVersion()) {
            throw new IllegalStateException("stale OAuth client version");
        }
        IdentityClientSecretEntity active = secrets
                .findActiveByClientIdForUpdate(client.getClientId())
                .orElseThrow(() -> new IllegalStateException(
                        "active OAuth client secret was not found"
                ));
        Instant now = clock.instant();
        byte[] randomBytes = new byte[32];
        char[] rawSecret = null;
        try {
            secureRandom.nextBytes(randomBytes);
            String plaintext = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(randomBytes);
            rawSecret = plaintext.toCharArray();
            String secretHash = passwordHashes.encode(rawSecret);
            String secretHint = plaintext.substring(plaintext.length() - 4);
            active.revoke(now);
            secrets.save(active);
            secrets.flush();
            client.rotateSecret(command.expectedVersion(), now);
            clients.save(client);
            secrets.save(IdentityClientSecretEntity.create(
                    String.valueOf(ids.nextId()),
                    client.getClientId(),
                    secretHash,
                    secretHint,
                    now
            ));
            RotatedClientSecretVO rotated = new RotatedClientSecretVO(
                    client.getClientId(),
                    client.getAppId(),
                    plaintext,
                    secretHint,
                    client.getVersion(),
                    now
            );
            securityEvents.append(new IdentitySecurityEvent(
                    "IDENTITY_OAUTH_CLIENT_SECRET_ROTATED",
                    operator,
                    "SECRET_ROTATED:" + client.getClientId(),
                    "ADMIN_API",
                    now
            ));
            return rotated;
        } finally {
            Arrays.fill(randomBytes, (byte) 0);
            if (rawSecret != null) {
                Arrays.fill(rawSecret, '\0');
            }
        }
    }

    /**
     * 生成并保存一个 Confidential Client 的初始 Secret。
     *
     * <p>Generates and persists the initial Secret for a Confidential Client.</p>
     */
    private CreatedOAuthClientVO createSecret(
            IdentityClientEntity client,
            Instant now
    ) {
        byte[] randomBytes = new byte[32];
        char[] rawSecret = null;
        try {
            secureRandom.nextBytes(randomBytes);
            String plaintext = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(randomBytes);
            rawSecret = plaintext.toCharArray();
            String secretHash = passwordHashes.encode(rawSecret);
            String secretHint = plaintext.substring(plaintext.length() - 4);
            secrets.save(IdentityClientSecretEntity.create(
                    String.valueOf(ids.nextId()),
                    client.getClientId(),
                    secretHash,
                    secretHint,
                    now
            ));
            secrets.flush();
            return new CreatedOAuthClientVO(
                    client.getClientId(),
                    client.getAppId(),
                    client.getClientName(),
                    client.getClientType().name(),
                    client.getStatus().name(),
                    plaintext,
                    secretHint,
                    client.getVersion(),
                    client.getCreatedAt()
            );
        } finally {
            Arrays.fill(randomBytes, (byte) 0);
            if (rawSecret != null) {
                Arrays.fill(rawSecret, '\0');
            }
        }
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
    public OAuthClientVO putResourceUri(
            String clientId,
            String resourceUri
    ) {
        IdentityClientEntity client = client(clientId);
        requirePublic(client);
        IdentityResourceServerEntity resource = resource(
                exact(resourceUri, "resourceUri"));
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
    public OAuthClientVO deleteResourceUri(
            String clientId,
            String resourceUri
    ) {
        IdentityClientEntity client = client(clientId);
        requirePublic(client);
        IdentityResourceServerEntity resource = resource(
                exact(resourceUri, "resourceUri")
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
     * @param resourceUris USER_DELEGATION Resource URI；USER_DELEGATION Resource URIs
     * @return Client 管理视图；Client management view
     */
    private OAuthClientVO view(
            IdentityClientEntity client,
            List<String> redirectUris,
            List<String> resourceUris
    ) {
        IdentityClientSecretEntity activeSecret = client.getClientType()
                == IdentityClientEntity.ClientType.CONFIDENTIAL
                ? secrets.findActiveByClientId(client.getClientId())
                .orElse(null)
                : null;
        return new OAuthClientVO(
                client.getClientId(),
                client.getClientName(),
                client.getClientType().name(),
                client.getStatus().name(),
                client.isPkceRequired(),
                client.getAccessTokenTtlSeconds(),
                client.getRefreshTokenTtlSeconds(),
                List.copyOf(redirectUris),
                List.copyOf(resourceUris),
                client.getVersion(),
                client.getCreatedAt(),
                client.getUpdatedAt(),
                client.getAppId(),
                activeSecret == null ? null : activeSecret.getSecretHint(),
                activeSecret == null ? null : activeSecret.getStatus().name()
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
        return resources.findByResourceUri(exact(resourceUri, "resourceUri"))
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

    /**
     * 校验审计操作者 Subject，避免事件端口收到无法持久化的值。
     *
     * <p>Validates the audit operator subject before publishing an event.</p>
     */
    private static String operator(String value) {
        String validated = exact(value, "operatorSub");
        if (!validated.matches("[A-Za-z0-9._~-]{1,64}")) {
            throw new IllegalArgumentException("operatorSub has invalid format");
        }
        return validated;
    }
}
