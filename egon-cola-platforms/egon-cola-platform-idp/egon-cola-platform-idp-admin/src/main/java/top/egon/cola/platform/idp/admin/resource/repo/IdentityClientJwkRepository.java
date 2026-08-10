package top.egon.cola.platform.idp.admin.resource.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityClientJwkEntity;

import java.util.List;
import java.util.Optional;

/**
 * OAuth Client 公开 JWK 仓储。
 *
 * <p>Repository for OAuth Client public JWKs.</p>
 */
public interface IdentityClientJwkRepository
        extends JpaRepository<IdentityClientJwkEntity, String> {

    /**
     * 查询 Client 的全部公开凭证。
     *
     * <p>Finds all public credentials for a Client.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @return 凭证列表；credential list
     */
    List<IdentityClientJwkEntity> findByClientId(String clientId);

    /**
     * 按 Client 和 kid 查询公开凭证。
     *
     * <p>Finds a public credential by Client and kid.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @param kid JWK kid；JWK kid
     * @return 凭证；credential
     */
    Optional<IdentityClientJwkEntity> findByClientIdAndKid(
            String clientId,
            String kid
    );
}
