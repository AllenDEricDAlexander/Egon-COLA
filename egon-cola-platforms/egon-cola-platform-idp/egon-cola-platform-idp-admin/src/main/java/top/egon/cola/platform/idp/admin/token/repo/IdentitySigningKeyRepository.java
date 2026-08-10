package top.egon.cola.platform.idp.admin.token.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.cola.platform.idp.admin.token.domain.pojo.IdentitySigningKeyEntity;

import java.util.List;

public interface IdentitySigningKeyRepository
        extends JpaRepository<IdentitySigningKeyEntity, String> {

    List<IdentitySigningKeyEntity> findByStatus(
            IdentitySigningKeyEntity.Status status
    );
}
