package top.egon.cola.platform.idp.admin.oauth.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.cola.platform.idp.admin.oauth.domain.IdentityClientAudienceEntity;

import java.util.List;

public interface IdentityClientAudienceRepository
        extends JpaRepository<IdentityClientAudienceEntity, String> {

    List<IdentityClientAudienceEntity> findByClientId(String clientId);
}
