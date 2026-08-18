package top.egon.cola.platform.rbac3.admin.iam.resource.service;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.iam.application.domain.vo.ApplicationVO;
import top.egon.cola.platform.rbac3.admin.iam.resource.domain.vo.ArchiveResultVO;
import top.egon.cola.platform.rbac3.admin.iam.resource.domain.vo.ResourceVO;
import top.egon.cola.platform.rbac3.admin.iam.resource.field.domain.dto.ChangeFieldDefinitionStatusRequestDTO;
import top.egon.cola.platform.rbac3.admin.iam.resource.field.domain.dto.CreateFieldDefinitionRequestDTO;
import top.egon.cola.platform.rbac3.admin.iam.resource.field.domain.vo.FieldDefinitionVO;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.shared.domain.DatabaseClock;

import java.time.Instant;
import java.util.List;

/** Global application/resource catalog queries and manual resource lifecycle operations. */
@Service
public final class GlobalResourceCatalogService {

    private final EntityManager entityManager;
    private final DatabaseClock clock;
    private final LongIdGenerator idGenerator;

    public GlobalResourceCatalogService(
            EntityManager entityManager,
            DatabaseClock clock,
            LongIdGenerator idGenerator) {
        this.entityManager = entityManager;
        this.clock = clock;
        this.idGenerator = idGenerator;
    }

    @Transactional(readOnly = true)
    public List<ApplicationVO> applications() {
        return entityManager.createNativeQuery("""
                        select id, application_code, application_name, status, version
                          from rbac3_application
                         order by display_priority, application_code
                        """)
                .getResultList().stream()
                .map(GlobalResourceCatalogService::application)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResourceVO> resources(String applicationId) {
        return entityManager.createNativeQuery("""
                        select id, application_id, resource_type, resource_code,
                               resource_name, parent_resource_id,
                               required_permission_id, status, version
                          from rbac3_resource
                         where application_id = :applicationId
                         order by resource_type, resource_code
                        """)
                .setParameter("applicationId", Long.valueOf(applicationId))
                .getResultList().stream()
                .map(GlobalResourceCatalogService::resource)
                .toList();
    }

    @Transactional
    public ArchiveResultVO archive(
            String resourceId,
            long expectedVersion,
            String actorId) {
        int updated = entityManager.createNativeQuery("""
                        update rbac3_resource
                           set status = 'ARCHIVED', version = version + 1,
                               updated_at = :now, updated_by = :actor
                         where id = :id and version = :version
                        """)
                .setParameter("now", clock.transactionNow())
                .setParameter("actor", actorId)
                .setParameter("id", Long.valueOf(resourceId))
                .setParameter("version", expectedVersion)
                .executeUpdate();
        if (updated != 1) {
            throw new IllegalStateException("resource version conflict or not found");
        }
        return new ArchiveResultVO(resourceId, "ARCHIVED", 0L);
    }

    @Transactional(readOnly = true)
    public List<FieldDefinitionVO> fields(String applicationId, String resourceId) {
        String resourcePredicate = resourceId == null ? "" : " and f.resource_id = :resourceId";
        var query = entityManager.createNativeQuery("""
                        select f.id, f.application_id, f.resource_id, f.field_code,
                               f.json_path, f.data_type, f.sensitivity, f.default_access,
                               f.masking_strategy, f.writable, f.exportable, f.status, f.version
                          from rbac3_field_definition f
                         where f.application_id = :applicationId
                        """ + resourcePredicate + " order by f.field_code")
                .setParameter("applicationId", Long.valueOf(applicationId));
        if (resourceId != null) {
            query.setParameter("resourceId", Long.valueOf(resourceId));
        }
        return ((List<?>) query.getResultList()).stream()
                .map(GlobalResourceCatalogService::field)
                .toList();
    }

    @Transactional
    public FieldDefinitionVO createField(
            CreateFieldDefinitionRequestDTO command,
            String actorId) {
        long applicationId = Long.parseLong(command.applicationId());
        long resourceId = Long.parseLong(command.resourceId());
        requireResource(applicationId, resourceId);
        Instant now = clock.transactionNow();
        long id = idGenerator.nextLongId();
        entityManager.createNativeQuery("""
                        insert into rbac3_field_definition (
                            id, application_id, resource_id, field_code, json_path,
                            data_type, sensitivity, default_access, masking_strategy,
                            writable, exportable, status, source_type, version,
                            created_at, created_by, updated_at, updated_by)
                        values (:id, :applicationId, :resourceId, :fieldCode, :jsonPath,
                                :dataType, :sensitivity, :defaultAccess, :maskingStrategy,
                                :writable, :exportable, 'ACTIVE', 'MANUAL', 0,
                                :now, :actor, :now, :actor)
                        """)
                .setParameter("id", id)
                .setParameter("applicationId", applicationId)
                .setParameter("resourceId", resourceId)
                .setParameter("fieldCode", command.fieldCode())
                .setParameter("jsonPath", command.jsonPath())
                .setParameter("dataType", command.dataType().toUpperCase())
                .setParameter("sensitivity", command.sensitivity().toUpperCase())
                .setParameter("defaultAccess", command.defaultAccess().toUpperCase())
                .setParameter("maskingStrategy", command.maskingStrategy())
                .setParameter("writable", command.writable())
                .setParameter("exportable", command.exportable())
                .setParameter("now", now)
                .setParameter("actor", actorId)
                .executeUpdate();
        return fields(Long.toString(applicationId), Long.toString(resourceId)).stream()
                .filter(value -> value.id().equals(Long.toString(id)))
                .findFirst().orElseThrow();
    }

    @Transactional
    public FieldDefinitionVO changeFieldStatus(
            String fieldId,
            ChangeFieldDefinitionStatusRequestDTO command,
            String actorId) {
        Instant now = clock.transactionNow();
        int updated = entityManager.createNativeQuery("""
                        update rbac3_field_definition
                           set status = :status, version = version + 1,
                               updated_at = :now, updated_by = :actor
                         where id = :id and version = :version
                        """)
                .setParameter("status", command.status().toUpperCase())
                .setParameter("now", now)
                .setParameter("actor", actorId)
                .setParameter("id", Long.valueOf(fieldId))
                .setParameter("version", command.expectedVersion())
                .executeUpdate();
        if (updated != 1) {
            throw new IllegalStateException("field definition version conflict or not found");
        }
        List<FieldDefinitionVO> values = fieldsForId(fieldId);
        return values.getFirst();
    }

    private List<FieldDefinitionVO> fieldsForId(String fieldId) {
        return ((List<?>) entityManager.createNativeQuery("""
                        select f.id, f.application_id, f.resource_id, f.field_code,
                               f.json_path, f.data_type, f.sensitivity, f.default_access,
                               f.masking_strategy, f.writable, f.exportable, f.status, f.version
                          from rbac3_field_definition f where f.id = :id
                        """)
                .setParameter("id", Long.valueOf(fieldId)).getResultList()).stream()
                .map(GlobalResourceCatalogService::field).toList();
    }

    private void requireResource(long applicationId, long resourceId) {
        Number count = (Number) entityManager.createNativeQuery(
                        "select count(*) from rbac3_resource where id = :resourceId and application_id = :applicationId")
                .setParameter("resourceId", resourceId)
                .setParameter("applicationId", applicationId)
                .getSingleResult();
        if (count.longValue() != 1L) {
            throw new IllegalStateException("global resource not found");
        }
    }

    private static ApplicationVO application(Object value) {
        Object[] row = (Object[]) value;
        return new ApplicationVO(
                String.valueOf(row[0]), String.valueOf(row[1]), String.valueOf(row[2]),
                String.valueOf(row[3]), ((Number) row[4]).longValue());
    }

    private static ResourceVO resource(Object value) {
        Object[] row = (Object[]) value;
        return new ResourceVO(
                String.valueOf(row[0]), String.valueOf(row[1]), String.valueOf(row[2]),
                String.valueOf(row[3]), String.valueOf(row[4]),
                row[5] == null ? null : String.valueOf(row[5]),
                row[6] == null ? null : String.valueOf(row[6]),
                String.valueOf(row[7]), ((Number) row[8]).longValue());
    }

    private static FieldDefinitionVO field(Object value) {
        Object[] row = (Object[]) value;
        return new FieldDefinitionVO(
                String.valueOf(row[0]), String.valueOf(row[1]), String.valueOf(row[2]),
                String.valueOf(row[3]), String.valueOf(row[4]), String.valueOf(row[5]),
                String.valueOf(row[6]), String.valueOf(row[7]),
                row[8] == null ? null : String.valueOf(row[8]),
                Boolean.TRUE.equals(row[9]), Boolean.TRUE.equals(row[10]),
                String.valueOf(row[11]), ((Number) row[12]).longValue());
    }
}
