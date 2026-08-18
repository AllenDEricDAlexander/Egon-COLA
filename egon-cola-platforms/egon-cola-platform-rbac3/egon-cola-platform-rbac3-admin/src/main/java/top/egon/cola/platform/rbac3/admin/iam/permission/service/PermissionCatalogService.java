package top.egon.cola.platform.rbac3.admin.iam.permission.service;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.iam.permission.domain.dto.ChangePermissionStatusRequestDTO;
import top.egon.cola.platform.rbac3.admin.iam.permission.domain.dto.CreatePermissionRequestDTO;
import top.egon.cola.platform.rbac3.admin.iam.permission.domain.vo.PermissionCatalogVO;
import top.egon.cola.platform.rbac3.admin.shared.domain.DatabaseClock;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Global permission CRUD; tenant role bindings are deliberately outside this service. */
@Service
public final class PermissionCatalogService {

    private final EntityManager entityManager;
    private final LongIdGenerator idGenerator;
    private final DatabaseClock clock;

    public PermissionCatalogService(
            EntityManager entityManager,
            LongIdGenerator idGenerator,
            DatabaseClock clock) {
        this.entityManager = entityManager;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<PermissionCatalogVO> list(String applicationId, boolean assignable) {
        String statusClause = assignable ? " and p.status = 'ACTIVE'" : "";
        List<?> rows = entityManager.createNativeQuery("""
                        select p.id, p.application_id, p.permission_code,
                               p.permission_name, p.risk_level, p.status,
                               p.source_type, p.source_build_id, p.source_checksum,
                               p.version
                          from rbac3_permission p
                         where p.application_id = :applicationId
                        """ + statusClause + " order by p.permission_code")
                .setParameter("applicationId", Long.valueOf(required(applicationId)))
                .getResultList();
        return rows.stream().map(PermissionCatalogService::view).toList();
    }

    @Transactional
    public PermissionCatalogVO create(CreatePermissionRequestDTO command, String actorId) {
        long applicationId = Long.parseLong(required(command.applicationId()));
        requireApplication(applicationId);
        String code = required(command.permissionCode());
        Instant now = clock.transactionNow();
        long id = idGenerator.nextLongId();
        entityManager.createNativeQuery("""
                        insert into rbac3_permission (
                            id, application_id, permission_code, permission_name,
                            risk_level, status, description, source_type, version,
                            created_at, created_by, updated_at, updated_by)
                        values (:id, :applicationId, :code, :name, :risk,
                                'PENDING_VALIDATION', :description, 'MANUAL', 0,
                                :now, :actor, :now, :actor)
                        """)
                .setParameter("id", id)
                .setParameter("applicationId", applicationId)
                .setParameter("code", code)
                .setParameter("name", required(command.permissionName()))
                .setParameter("risk", required(command.riskLevel()).toUpperCase())
                .setParameter("description", command.description())
                .setParameter("now", now)
                .setParameter("actor", required(actorId))
                .executeUpdate();
        return find(Long.toString(id));
    }

    @Transactional
    public PermissionCatalogVO changeStatus(
            String id,
            ChangePermissionStatusRequestDTO command,
            String actorId) {
        Instant now = clock.transactionNow();
        int updated = entityManager.createNativeQuery("""
                        update rbac3_permission
                           set status = :status, version = version + 1,
                               updated_at = :now, updated_by = :actor
                         where id = :id and version = :version
                        """)
                .setParameter("status", required(command.status()).toUpperCase())
                .setParameter("now", now)
                .setParameter("actor", required(actorId))
                .setParameter("id", Long.valueOf(required(id)))
                .setParameter("version", command.expectedVersion())
                .executeUpdate();
        if (updated != 1) {
            throw new IllegalStateException("permission version conflict or not found");
        }
        return find(id);
    }

    @Transactional(readOnly = true)
    public PermissionCatalogVO find(String id) {
        List<?> rows = entityManager.createNativeQuery("""
                        select p.id, p.application_id, p.permission_code,
                               p.permission_name, p.risk_level, p.status,
                               p.source_type, p.source_build_id, p.source_checksum,
                               p.version
                          from rbac3_permission p where p.id = :id
                        """)
                .setParameter("id", Long.valueOf(required(id)))
                .getResultList();
        if (rows.size() != 1) {
            throw new IllegalStateException("permission not found");
        }
        return view(rows.getFirst());
    }

    private void requireApplication(long applicationId) {
        Number count = (Number) entityManager.createNativeQuery(
                        "select count(*) from rbac3_application where id = :id")
                .setParameter("id", applicationId)
                .getSingleResult();
        if (count.longValue() != 1L) {
            throw new IllegalStateException("global application not found");
        }
    }

    private static PermissionCatalogVO view(Object value) {
        Object[] row = (Object[]) value;
        return new PermissionCatalogVO(
                String.valueOf(row[0]), String.valueOf(row[1]), String.valueOf(row[2]),
                String.valueOf(row[3]), String.valueOf(row[4]), String.valueOf(row[5]),
                String.valueOf(row[6]), row[7] == null ? null : String.valueOf(row[7]),
                row[8] == null ? null : String.valueOf(row[8]), ((Number) row[9]).longValue());
    }

    private static String required(String value) {
        Objects.requireNonNull(value, "value");
        if (value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException("value is required");
        }
        return value;
    }
}
