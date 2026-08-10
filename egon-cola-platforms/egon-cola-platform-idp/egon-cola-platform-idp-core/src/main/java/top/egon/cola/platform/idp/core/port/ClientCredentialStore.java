package top.egon.cola.platform.idp.core.port;

import top.egon.cola.platform.idp.core.resource.ClientJwkCredential;

import java.util.List;
import java.util.Optional;

/**
 * OAuth Client 公开 JWK 凭证查询端口。
 *
 * <p>Lookup port for OAuth Client public JWK credentials.</p>
 */
public interface ClientCredentialStore {

    /**
     * 按 Client 和 kid 查询凭证。
     *
     * <p>Finds a credential by Client and kid.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @param keyId    JWK kid；JWK kid
     * @return 公开凭证；public credential
     */
    Optional<ClientJwkCredential> findByClientIdAndKeyId(
            String clientId,
            String keyId);

    /**
     * 查询 Client 的全部凭证。
     *
     * <p>Lists all credentials for a Client.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @return 不可变凭证列表；immutable credential list
     */
    List<ClientJwkCredential> findByClientId(String clientId);
}
