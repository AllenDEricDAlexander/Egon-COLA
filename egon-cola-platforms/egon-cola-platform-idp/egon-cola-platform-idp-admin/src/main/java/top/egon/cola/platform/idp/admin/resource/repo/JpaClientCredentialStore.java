package top.egon.cola.platform.idp.admin.resource.repo;

import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityClientJwkEntity;
import top.egon.cola.platform.idp.core.port.ClientCredentialStore;
import top.egon.cola.platform.idp.core.resource.ClientJwkCredential;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 从 IdP 管理库查询 OAuth Client 公开 JWK 的领域适配器。
 *
 * <p>Domain adapter that queries OAuth Client public JWKs from the IdP administration database.</p>
 */
public final class JpaClientCredentialStore implements ClientCredentialStore {

    /** Client JWK 仓储；Client JWK repository. */
    private final IdentityClientJwkRepository credentials;

    /**
     * 创建 Client 公开凭证查询适配器。
     *
     * <p>Creates the Client public-credential lookup adapter.</p>
     *
     * @param credentials Client JWK 仓储；Client JWK repository
     */
    public JpaClientCredentialStore(
            IdentityClientJwkRepository credentials
    ) {
        this.credentials = Objects.requireNonNull(
                credentials,
                "credentials"
        );
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public Optional<ClientJwkCredential> findByClientIdAndKeyId(
            String clientId,
            String keyId
    ) {
        return credentials.findByClientIdAndKid(clientId, keyId)
                .map(JpaClientCredentialStore::toDomain);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<ClientJwkCredential> findByClientId(String clientId) {
        return credentials.findByClientId(clientId).stream()
                .map(JpaClientCredentialStore::toDomain)
                .toList();
    }

    /**
     * 将持久化 JWK 映射为领域凭证。
     *
     * <p>Maps a persisted JWK to the domain credential.</p>
     *
     * @param entity 持久化 JWK；persisted JWK
     * @return 领域凭证；domain credential
     */
    private static ClientJwkCredential toDomain(
            IdentityClientJwkEntity entity
    ) {
        return new ClientJwkCredential(
                entity.getClientId(),
                entity.getKid(),
                entity.getAlgorithm(),
                entity.getPublicJwk(),
                entity.getValidFrom(),
                entity.getValidTo(),
                ClientJwkCredential.Status.valueOf(
                        entity.getStatus().name()
                ),
                entity.getLastUsedAt(),
                entity.getVersion()
        );
    }
}
