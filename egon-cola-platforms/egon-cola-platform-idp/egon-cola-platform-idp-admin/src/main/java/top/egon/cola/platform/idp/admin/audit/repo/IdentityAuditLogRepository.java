package top.egon.cola.platform.idp.admin.audit.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.cola.platform.idp.admin.audit.domain.pojo.IdentityAuditLogEntity;

public interface IdentityAuditLogRepository
        extends JpaRepository<IdentityAuditLogEntity, String> {
}
