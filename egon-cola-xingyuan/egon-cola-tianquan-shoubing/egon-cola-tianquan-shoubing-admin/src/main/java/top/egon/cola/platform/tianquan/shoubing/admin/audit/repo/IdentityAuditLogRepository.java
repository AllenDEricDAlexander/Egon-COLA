package top.egon.cola.platform.tianquan.shoubing.admin.audit.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.pojo.IdentityAuditLogEntity;

public interface IdentityAuditLogRepository
        extends JpaRepository<IdentityAuditLogEntity, String>,
        JpaSpecificationExecutor<IdentityAuditLogEntity> {
}
