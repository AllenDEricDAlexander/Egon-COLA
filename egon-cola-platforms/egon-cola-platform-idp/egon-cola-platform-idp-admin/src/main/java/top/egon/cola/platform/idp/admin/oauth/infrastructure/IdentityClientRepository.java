package top.egon.cola.platform.idp.admin.oauth.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.cola.platform.idp.admin.oauth.domain.IdentityClientEntity;

public interface IdentityClientRepository
        extends JpaRepository<IdentityClientEntity, String> {
}
