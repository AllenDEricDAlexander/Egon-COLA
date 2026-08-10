package top.egon.cola.platform.idp.admin.oauth.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientEntity;

public interface IdentityClientRepository
        extends JpaRepository<IdentityClientEntity, String> {
}
