package top.egon.cola.platform.idp.admin.oauth.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientAudienceEntity;

import java.util.List;

public interface IdentityClientAudienceRepository
        extends JpaRepository<IdentityClientAudienceEntity, String> {

    List<IdentityClientAudienceEntity> findByClientId(String clientId);

    boolean existsByClientIdAndAudience(String clientId, String audience);

    void deleteByClientIdAndAudience(String clientId, String audience);
}
