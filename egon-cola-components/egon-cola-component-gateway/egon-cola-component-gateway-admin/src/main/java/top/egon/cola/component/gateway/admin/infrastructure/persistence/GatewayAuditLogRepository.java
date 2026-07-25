package top.egon.cola.component.gateway.admin.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GatewayAuditLogRepository
        extends JpaRepository<GatewayAuditLogEntity, String> {
}
