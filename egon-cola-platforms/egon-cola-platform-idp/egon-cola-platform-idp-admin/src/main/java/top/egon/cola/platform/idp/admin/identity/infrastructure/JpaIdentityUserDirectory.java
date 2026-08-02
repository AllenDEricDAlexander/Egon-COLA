package top.egon.cola.platform.idp.admin.identity.infrastructure;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.idp.admin.identity.application.IdentityUserAdminService;
import top.egon.cola.platform.idp.admin.identity.domain.IdentityUserEntity;
import top.egon.cola.platform.idp.core.identity.IdentityUser;

import java.util.List;
import java.util.Objects;

@Repository
public class JpaIdentityUserDirectory
        implements IdentityUserAdminService.UserDirectory {

    private final EntityManager entityManager;

    public JpaIdentityUserDirectory(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(
                entityManager,
                "entityManager"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<IdentityUser> list() {
        return entityManager.createQuery("""
                        select u
                          from IdentityUserEntity u
                         order by u.normalizedUsername, u.id
                        """, IdentityUserEntity.class)
                .getResultList()
                .stream()
                .map(IdentityUserEntity::toDomain)
                .toList();
    }
}
