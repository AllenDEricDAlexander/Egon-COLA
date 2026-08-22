package top.egon.cola.platform.idp.admin.tenant.repo;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import top.egon.cola.platform.idp.admin.tenant.domain.pojo.IdentityTenantEntity;

import java.util.List;
import java.util.Optional;

/** Persistence access for IdP tenant catalog facts. */
@Repository
public interface IdentityTenantRepository
        extends JpaRepository<IdentityTenantEntity, String> {

    boolean existsByTenantCodeIgnoreCase(String tenantCode);

    Optional<IdentityTenantEntity> findByTenantCodeIgnoreCase(
            String tenantCode
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query(
            "select t from IdentityTenantEntity t where t.id = :id"
    )
    Optional<IdentityTenantEntity> findByIdForUpdate(
            @org.springframework.data.repository.query.Param("id") String id
    );

    List<IdentityTenantEntity> findAllByOrderByUpdatedAtDescIdAsc();
}
