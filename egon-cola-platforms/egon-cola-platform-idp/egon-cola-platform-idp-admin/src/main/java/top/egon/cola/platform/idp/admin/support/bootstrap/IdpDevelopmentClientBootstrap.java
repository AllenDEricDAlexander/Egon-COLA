package top.egon.cola.platform.idp.admin.support.bootstrap;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import top.egon.cola.platform.idp.admin.oauth.domain.dto.CreateOAuthClientDTO;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.OAuthClientVO;
import top.egon.cola.platform.idp.admin.oauth.service.OAuthClientService;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityClientResourceGrantEntity;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityResourceServerEntity;
import top.egon.cola.platform.idp.admin.resource.repo.IdentityClientResourceGrantRepository;
import top.egon.cola.platform.idp.admin.resource.repo.IdentityResourceServerRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 幂等注册本地拓扑使用的 OAuth Client、Resource Server 和应用级授权。
 *
 * <p>Idempotently registers OAuth Clients, Resource Servers, and application-level grants used
 * by the explicit local topology.</p>
 */
@Component
@Profile("local")
@ConditionalOnProperty(
        prefix = "egon.idp.development-bootstrap",
        name = "enabled",
        havingValue = "true")
public class IdpDevelopmentClientBootstrap implements ApplicationRunner {

    /** 开发 Client 的 Access Token 有效秒数；development access-token lifetime in seconds. */
    private static final int ACCESS_TOKEN_TTL_SECONDS = 900;

    /** 开发 Client 的 Refresh Token 有效秒数；development refresh-token lifetime in seconds. */
    private static final int REFRESH_TOKEN_TTL_SECONDS = 604_800;

    /** 开发环境中需要幂等创建的 Public Client；public Clients created idempotently for local use. */
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

    /** 开发环境明确审批的应用级 Resource Server；explicitly approved local Resource Servers. */
    private static final List<ResourceSpec> RESOURCES = List.of(
            new ResourceSpec(
                    "permission-idp-local",
                    "https://api.egon.internal/local/permission/idp",
                    "idp",
                    "IdP Local",
                    "idp-admin-web",
                    "idp",
                    "idp:access"
            ),
            new ResourceSpec(
                    "permission-rbac3-local",
                    "https://api.egon.internal/local/permission/rbac3",
                    "rbac3",
                    "RBAC3 Local",
                    "rbac3-admin-web",
                    "rbac3",
                    "rbac3:access"
            )
    );

    /** OAuth Client 管理服务；OAuth Client management service. */
    private final OAuthClientService clients;

    /** Resource Server 仓储；Resource Server repository. */
    private final IdentityResourceServerRepository resources;

    /** Client Resource Grant 仓储；Client Resource Grant repository. */
    private final IdentityClientResourceGrantRepository grants;

    /**
     * 创建开发拓扑初始化器。
     *
     * <p>Creates the development-topology bootstrap.</p>
     *
     * @param clients OAuth Client 管理服务；OAuth Client management service
     * @param resources Resource Server 仓储；Resource Server repository
     * @param grants Client Resource Grant 仓储；Client Resource Grant repository
     */
    public IdpDevelopmentClientBootstrap(
            OAuthClientService clients,
            IdentityResourceServerRepository resources,
            IdentityClientResourceGrantRepository grants
    ) {
        this.clients = Objects.requireNonNull(clients, "clients");
        this.resources = Objects.requireNonNull(resources, "resources");
        this.grants = Objects.requireNonNull(grants, "grants");
    }

    /**
     * 幂等对齐开发 Client，再显式创建两个 Resource 和对应 USER_DELEGATION 授权。
     *
     * <p>Reconciles development Clients, then explicitly creates two Resources and their
     * USER_DELEGATION grants.</p>
     *
     * @param arguments 启动参数；application arguments
     */
    @Override
    public void run(ApplicationArguments arguments) {
        Map<String, OAuthClientVO> existing =
                clients.list().stream().collect(Collectors.toUnmodifiableMap(
                        OAuthClientVO::clientId,
                        client -> client
                ));
        CLIENTS.forEach(client -> reconcile(client, existing.get(
                client.clientId()
        )));
        RESOURCES.forEach(this::reconcileResourceAndGrant);
    }

    /**
     * 对齐一个开发 Client 的创建和回调地址。
     *
     * <p>Reconciles creation and redirect URIs for one development Client.</p>
     *
     * @param client 期望 Client；desired Client
     * @param existing 已有 Client 或空；existing Client or {@code null}
     */
    private void reconcile(
            ClientSpec client,
            OAuthClientVO existing) {
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

    /**
     * 创建一个尚不存在的开发 Client。
     *
     * <p>Creates one missing development Client.</p>
     *
     * @param client Client 规格；Client specification
     */
    private void create(ClientSpec client) {
        clients.create(new CreateOAuthClientDTO(
                client.clientId(),
                client.clientName(),
                ACCESS_TOKEN_TTL_SECONDS,
                REFRESH_TOKEN_TTL_SECONDS,
                List.of(redirectUri(client)),
                List.of()));
    }

    /**
     * 生成开发 Client 回调地址。
     *
     * <p>Builds a development Client redirect URI.</p>
     *
     * @param client Client 规格；Client specification
     * @return 回调地址；redirect URI
     */
    private String redirectUri(ClientSpec client) {
        return "http://127.0.0.1:" + client.port() + "/oauth/callback";
    }

    /**
     * 幂等创建一个 Resource Server 和对应 Public Client 的应用级授权。
     *
     * <p>Idempotently creates one Resource Server and its Public Client application grant.</p>
     *
     * @param spec Resource 规格；Resource specification
     */
    private void reconcileResourceAndGrant(ResourceSpec spec) {
        IdentityResourceServerEntity resource = resources
                .findByResourceServerId(spec.resourceServerId())
                .orElseGet(() -> createResource(spec));
        if (!grants.existsByClientIdAndResourceServerIdAndGrantType(
                spec.managementClientId(),
                resource.getResourceServerId(),
                IdentityClientResourceGrantEntity.GrantType.USER_DELEGATION
        )) {
            grants.save(IdentityClientResourceGrantEntity.userDelegation(
                    "dev-user-grant-" + spec.appCode(),
                    spec.managementClientId(),
                    resource.getResourceServerId(),
                    Instant.now()
            ));
        }
    }

    /**
     * 创建 ACTIVE 开发 Resource Server。
     *
     * <p>Creates an ACTIVE development Resource Server.</p>
     *
     * @param spec Resource 规格；Resource specification
     * @return 新 Resource Server；new Resource Server
     */
    private IdentityResourceServerEntity createResource(ResourceSpec spec) {
        IdentityResourceServerEntity resource =
                IdentityResourceServerEntity.create(
                        "dev-resource-" + spec.appCode(),
                        spec.resourceServerId(),
                        spec.resourceUri(),
                        "permission",
                        spec.appCode(),
                        "local",
                        spec.displayName(),
                        spec.managementClientId(),
                        spec.rbacApplicationCode(),
                        spec.entryPermissionCode(),
                        300,
                        IdentityResourceServerEntity.Status.ACTIVE,
                        Instant.now()
                );
        resources.save(resource);
        return resource;
    }

    /**
     * 开发 Public Client 规格。
     *
     * <p>Development Public Client specification.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @param clientName 展示名称；display name
     * @param port 本地回调端口；local redirect port
     * @param obsoleteRedirectUris 需要删除的旧回调；obsolete redirects to remove
     */
    private record ClientSpec(
            String clientId,
            String clientName,
            int port,
            List<String> obsoleteRedirectUris
    ) {
        /**
         * 创建没有旧回调地址的 Client 规格。
         *
         * <p>Creates a Client specification without obsolete redirects.</p>
         *
         * @param clientId Client 标识；Client identifier
         * @param clientName 展示名称；display name
         * @param port 本地回调端口；local redirect port
         */
        private ClientSpec(String clientId, String clientName, int port) {
            this(clientId, clientName, port, List.of());
        }
    }

    /**
     * 开发 Resource Server 规格。
     *
     * <p>Development Resource Server specification.</p>
     *
     * @param resourceServerId Resource Server 标识；Resource Server identifier
     * @param resourceUri Resource URI；Resource URI
     * @param appCode 应用编码；application code
     * @param displayName 展示名称；display name
     * @param managementClientId 管理 Client；management Client
     * @param rbacApplicationCode RBAC3 应用；RBAC3 application
     * @param entryPermissionCode 入口权限；entry permission
     */
    private record ResourceSpec(
            String resourceServerId,
            String resourceUri,
            String appCode,
            String displayName,
            String managementClientId,
            String rbacApplicationCode,
            String entryPermissionCode
    ) {
    }
}
