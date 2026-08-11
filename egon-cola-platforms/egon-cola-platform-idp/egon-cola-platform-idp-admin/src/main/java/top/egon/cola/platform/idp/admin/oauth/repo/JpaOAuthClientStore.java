package top.egon.cola.platform.idp.admin.oauth.repo;

import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientEntity;
import top.egon.cola.platform.idp.core.oauth.OAuthClient;
import top.egon.cola.platform.idp.core.port.OAuthClientStore;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * 从 IdP 管理库装载 OAuth Client 协议配置。
 *
 * <p>Loads OAuth Client protocol configuration from the IdP administration database.</p>
 *
 * <p>Resource Server 访问许可由独立的 {@code ResourceServerStore} 提供，Client 本身不再携带
 * 静态 audience 列表。</p>
 *
 * <p>Resource Server access grants are supplied by a separate {@code ResourceServerStore}; the
 * Client no longer carries a static audience list.</p>
 */
public class JpaOAuthClientStore implements OAuthClientStore {

    /** Client 主记录仓储；Client master-record repository. */
    private final IdentityClientRepository clients;

    /** Client 回调地址仓储；Client redirect-URI repository. */
    private final IdentityClientRedirectUriRepository redirects;

    /**
     * 创建 JPA OAuth Client 查询适配器。
     *
     * <p>Creates the JPA OAuth Client lookup adapter.</p>
     *
     * @param clients Client 主记录仓储；Client master-record repository
     * @param redirects Client 回调地址仓储；Client redirect-URI repository
     */
    public JpaOAuthClientStore(
            IdentityClientRepository clients,
            IdentityClientRedirectUriRepository redirects
    ) {
        this.clients = Objects.requireNonNull(clients, "clients");
        this.redirects = Objects.requireNonNull(redirects, "redirects");
    }

    /**
     * 按 Client 标识查询当前协议配置。
     *
     * <p>Finds the current protocol configuration by Client identifier.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @return Client 存在时返回领域对象；domain Client when present
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<OAuthClient> findById(String clientId) {
        return clients.findById(clientId).map(this::toDomain);
    }

    /**
     * 将持久化 Client 映射为协议领域对象。
     *
     * <p>Maps a persisted Client to the protocol domain object.</p>
     *
     * @param entity Client 持久化对象；Client persistence object
     * @return OAuth Client 领域对象；OAuth Client domain object
     */
    private OAuthClient toDomain(IdentityClientEntity entity) {
        return new OAuthClient(
                entity.getClientId(),
                OAuthClient.ClientType.valueOf(entity.getClientType().name()),
                OAuthClient.Status.valueOf(entity.getStatus().name()),
                entity.isPkceRequired(),
                redirects.findByClientId(entity.getClientId()).stream()
                        .map(value -> value.getRedirectUri())
                        .toList(),
                Duration.ofSeconds(entity.getAccessTokenTtlSeconds()),
                Duration.ofSeconds(entity.getRefreshTokenTtlSeconds())
        );
    }
}
