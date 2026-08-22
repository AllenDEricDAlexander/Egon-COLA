package top.egon.cola.platform.idp.admin.tenant.repo;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import top.egon.cola.platform.idp.admin.tenant.domain.pojo.IdentityTenantMembershipEntity;

import java.util.List;
import java.util.Optional;

/** Persistence access for IdP tenant membership facts. */
@Repository
public interface IdentityTenantMembershipRepository
        extends JpaRepository<IdentityTenantMembershipEntity, String> {

    Optional<IdentityTenantMembershipEntity>
    findByTenantIdAndIdentitySub(String tenantId, String identitySub);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select m
              from IdentityTenantMembershipEntity m
             where m.tenantId = :tenantId
               and m.identitySub = :identitySub
            """)
    Optional<IdentityTenantMembershipEntity> findByTenantIdAndIdentitySubForUpdate(
            @Param("tenantId") String tenantId,
            @Param("identitySub") String identitySub
    );

    List<IdentityTenantMembershipEntity>
    findByTenantIdOrderByUpdatedAtDescIdentitySubAsc(String tenantId);

    List<IdentityTenantMembershipEntity>
    findByIdentitySubOrderByUpdatedAtDescTenantIdAsc(String identitySub);
}
