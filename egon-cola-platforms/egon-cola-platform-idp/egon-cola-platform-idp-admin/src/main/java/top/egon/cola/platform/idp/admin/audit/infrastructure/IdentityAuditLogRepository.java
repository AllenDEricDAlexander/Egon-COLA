package top.egon.cola.platform.idp.admin.audit.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.cola.platform.idp.admin.audit.domain.IdentityAuditLogEntity;

public interface IdentityAuditLogRepository
        extends JpaRepository<IdentityAuditLogEntity, String> {
}
