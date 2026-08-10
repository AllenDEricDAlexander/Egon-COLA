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

    /**
     * 判断 Client 是否至少拥有一个指定状态的公开凭证。
     *
     * <p>Checks whether a Client has at least one public credential in the given status.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @param status 凭证状态；credential status
     * @return 存在时为 {@code true}；{@code true} when present
     */
    boolean existsByClientIdAndStatus(
            String clientId,
            IdentityClientJwkEntity.Status status
    );

    /**
     * 统计 Client 指定状态的凭证。
     *
     * <p>Counts Client credentials in the given status.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @param status 凭证状态；credential status
     * @return 凭证数量；credential count
     */
    long countByClientIdAndStatus(
            String clientId,
            IdentityClientJwkEntity.Status status
    );
}
