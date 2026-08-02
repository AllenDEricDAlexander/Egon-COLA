package top.egon.cola.platform.idp.admin.oauth.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.cola.platform.idp.admin.oauth.domain.IdentityClientRedirectUriEntity;

import java.util.List;

public interface IdentityClientRedirectUriRepository
        extends JpaRepository<IdentityClientRedirectUriEntity, String> {

    List<IdentityClientRedirectUriEntity> findByClientId(String clientId);

    boolean existsByClientIdAndRedirectUri(String clientId, String redirectUri);

    void deleteByClientIdAndRedirectUri(String clientId, String redirectUri);
}
