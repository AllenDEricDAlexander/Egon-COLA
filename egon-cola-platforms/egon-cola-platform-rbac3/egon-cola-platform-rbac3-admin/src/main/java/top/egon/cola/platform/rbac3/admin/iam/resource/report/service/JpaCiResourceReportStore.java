package top.egon.cola.platform.rbac3.admin.iam.resource.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.iam.resource.report.domain.dto.CiResourceReportRequestDTO;
import top.egon.cola.platform.rbac3.admin.iam.resource.report.domain.dto.CiResourceReportRequestDTO.Field;
import top.egon.cola.platform.rbac3.admin.iam.resource.report.domain.dto.CiResourceReportRequestDTO.Resource;
import top.egon.cola.platform.rbac3.admin.iam.resource.report.domain.vo.CiResourceReportResultVO;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** PostgreSQL transaction boundary for global CI_REPORT facts. */
@Repository
public class JpaCiResourceReportStore implements CiResourceReportStore {

    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    private final LongIdGenerator idGenerator;

    public JpaCiResourceReportStore(
            EntityManager entityManager,
            ObjectMapper objectMapper,
            LongIdGenerator idGenerator) {
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
        this.idGenerator = idGenerator;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReportHead> findHead(String applicationCode) {
        List<?> rows = entityManager.createNativeQuery("""
                        select ci_report_build_id, ci_report_checksum,
                               version
                          from rbac3_application
                         where application_code = :applicationCode
                        """)
                .setParameter("applicationCode", applicationCode)
                .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = (Object[]) rows.getFirst();
        if (row[0] == null || row[1] == null) {
            return Optional.empty();
        }
        return Optional.of(new ReportHead(
                String.valueOf(row[0]),
                String.valueOf(row[1]),
                new CiResourceReportResultVO(0, 0, 0, 0, 0, String.valueOf(row[1])),
                ((Number) row[2]).longValue()));
    }

    @Override
    @Transactional
    public CiResourceReportResultVO replace(
            String applicationCode,
            CiResourceReportRequestDTO request,
            String checksum) {
        Long applicationId = applicationId(applicationCode);
        Instant now = Instant.now();
        int added = 0;
        int updated = 0;
        int pending = 0;
        for (Resource resource : request.resources()) {
            Long permissionId = permissionId(applicationId, resource.permissionCode(), request, now);
            boolean exists = resourceExists(applicationId, resource);
            upsertResource(applicationId, resource, permissionId, request.buildId(), checksum, now);
            if (exists) {
                updated++;
            } else {
                added++;
            }
            if (permissionId != null) {
                pending++;
            }
        }
        for (Field field : request.fields()) {
            boolean exists = fieldExists(applicationId, field);
            upsertField(applicationId, field, request.buildId(), checksum, now);
            if (exists) {
                updated++;
            } else {
                added++;
            }
            pending++;
        }
        int stale = markStale(applicationId, request, now);
        CiResourceReportResultVO result = new CiResourceReportResultVO(
                added, updated, stale, 0, pending, checksum);
        entityManager.createNativeQuery("""
                        update rbac3_application
                           set ci_report_build_id = :buildId,
                               ci_report_checksum = :checksum,
                               ci_reported_at = :reportedAt,
                               version = version + 1,
                               updated_at = :reportedAt,
                               updated_by = 'ci-report'
                         where id = :applicationId
                        """)
                .setParameter("buildId", request.buildId())
                .setParameter("checksum", checksum)
                .setParameter("reportedAt", now)
                .setParameter("applicationId", applicationId)
                .executeUpdate();
        return result;
    }

    private Long applicationId(String applicationCode) {
        List<?> rows = entityManager.createNativeQuery("""
                        select id from rbac3_application
                         where application_code = :applicationCode
                        """)
                .setParameter("applicationCode", applicationCode)
                .getResultList();
        if (rows.size() != 1) {
            throw new IllegalStateException("global application not found");
        }
        return ((Number) rows.getFirst()).longValue();
    }

    private Long permissionId(
            Long applicationId,
            String permissionCode,
            CiResourceReportRequestDTO request,
            Instant now) {
        if (permissionCode == null) {
            return null;
        }
        List<?> rows = entityManager.createNativeQuery("""
                        select id from rbac3_permission
                         where permission_code = :permissionCode
                        """)
                .setParameter("permissionCode", permissionCode)
                .getResultList();
        if (!rows.isEmpty()) {
            return ((Number) rows.getFirst()).longValue();
        }
        long id = idGenerator.nextLongId();
        entityManager.createNativeQuery("""
                        insert into rbac3_permission (
                            id, application_id, permission_code, permission_name,
                            risk_level, status, description, source_type,
                            source_build_id, source_checksum, ci_reported_at,
                            version, created_at, created_by, updated_at, updated_by)
                        values (:id, :applicationId, :code, :name, 'LOW',
                                'PENDING_VALIDATION', null, 'CI_REPORT',
                                :buildId, :checksum, :reportedAt, 0,
                                :reportedAt, 'ci-report', :reportedAt, 'ci-report')
                        """)
                .setParameter("id", id)
                .setParameter("applicationId", applicationId)
                .setParameter("code", permissionCode)
                .setParameter("name", permissionCode)
                .setParameter("buildId", request.buildId())
                .setParameter("checksum", CiResourceReportCanonicalizer.checksum(request))
                .setParameter("reportedAt", now)
                .executeUpdate();
        return id;
    }

    private boolean resourceExists(Long applicationId, Resource resource) {
        return !entityManager.createNativeQuery("""
                        select 1 from rbac3_resource
                         where application_id = :applicationId
                           and resource_type = :type
                           and resource_code = :code
                        """)
                .setParameter("applicationId", applicationId)
                .setParameter("type", resource.type().name())
                .setParameter("code", resource.code())
                .getResultList().isEmpty();
    }

    private void upsertResource(
            Long applicationId,
            Resource resource,
            Long permissionId,
            String buildId,
            String checksum,
            Instant now) {
        String facts = json(Map.of(
                "path", value(resource.path()),
                "componentKey", value(resource.componentKey()),
                "routeCode", value(resource.routeCode()),
                "order", value(resource.order()),
                "hidden", resource.hidden()));
        String metadata = json(Map.of("name", resource.name()));
        entityManager.createNativeQuery("""
                        insert into rbac3_resource (
                            id, application_id, resource_type, resource_code,
                            resource_name, parent_resource_id, required_permission_id,
                            status, source_type, source_build_id, source_checksum,
                            mechanical_facts, display_metadata, version,
                            created_at, created_by, updated_at, updated_by)
                        values (:id, :applicationId, :type, :code, :name,
                                (select id from rbac3_resource where application_id = :applicationId
                                  and resource_code = :parentCode limit 1),
                                :permissionId, 'PENDING_VALIDATION', 'CI_REPORT',
                                :buildId, :checksum, cast(:facts as jsonb),
                                cast(:metadata as jsonb), 0, :now, 'ci-report', :now, 'ci-report')
                        on conflict (application_id, resource_type, resource_code)
                        do update set resource_name = excluded.resource_name,
                            required_permission_id = excluded.required_permission_id,
                            source_type = 'CI_REPORT', source_build_id = excluded.source_build_id,
                            source_checksum = excluded.source_checksum,
                            mechanical_facts = excluded.mechanical_facts,
                            display_metadata = excluded.display_metadata,
                            updated_at = excluded.updated_at, updated_by = excluded.updated_by
                        """)
                .setParameter("id", idGenerator.nextLongId())
                .setParameter("applicationId", applicationId)
                .setParameter("type", resource.type().name())
                .setParameter("code", resource.code())
                .setParameter("name", resource.name())
                .setParameter("parentCode", resource.parentCode())
                .setParameter("permissionId", permissionId)
                .setParameter("buildId", buildId)
                .setParameter("checksum", checksum)
                .setParameter("facts", facts)
                .setParameter("metadata", metadata)
                .setParameter("now", now)
                .executeUpdate();
    }

    private boolean fieldExists(Long applicationId, Field field) {
        return !entityManager.createNativeQuery("""
                        select 1 from rbac3_field_definition f
                         join rbac3_resource r on r.id = f.resource_id
                         where f.application_id = :applicationId
                           and r.resource_code = :resourceCode
                           and f.field_code = :fieldCode
                        """)
                .setParameter("applicationId", applicationId)
                .setParameter("resourceCode", field.resourceCode())
                .setParameter("fieldCode", field.fieldCode())
                .getResultList().isEmpty();
    }

    private void upsertField(
            Long applicationId,
            Field field,
            String buildId,
            String checksum,
            Instant now) {
        entityManager.createNativeQuery("""
                        insert into rbac3_field_definition (
                            id, application_id, resource_id, field_code, json_path,
                            data_type, sensitivity, default_access, masking_strategy,
                            writable, exportable, status, source_type, source_build_id,
                            source_checksum, ci_reported_at, version,
                            created_at, created_by, updated_at, updated_by)
                        values (:id, :applicationId,
                                (select id from rbac3_resource where application_id = :applicationId
                                  and resource_code = :resourceCode limit 1),
                                :fieldCode, :jsonPath, :dataType, 'NORMAL', 'NONE',
                                null, false, false, 'PENDING_VALIDATION', 'CI_REPORT',
                                :buildId, :checksum, :now, 0, :now, 'ci-report', :now, 'ci-report')
                        on conflict (application_id, resource_id, field_code)
                        do update set json_path = excluded.json_path,
                            data_type = excluded.data_type,
                            source_type = 'CI_REPORT', source_build_id = excluded.source_build_id,
                            source_checksum = excluded.source_checksum,
                            ci_reported_at = excluded.ci_reported_at,
                            updated_at = excluded.updated_at, updated_by = excluded.updated_by
                        """)
                .setParameter("id", idGenerator.nextLongId())
                .setParameter("applicationId", applicationId)
                .setParameter("resourceCode", field.resourceCode())
                .setParameter("fieldCode", field.fieldCode())
                .setParameter("jsonPath", field.jsonPath())
                .setParameter("dataType", field.dataType())
                .setParameter("buildId", buildId)
                .setParameter("checksum", checksum)
                .setParameter("now", now)
                .executeUpdate();
    }

    private int markStale(
            Long applicationId,
            CiResourceReportRequestDTO request,
            Instant now) {
        List<String> codes = request.resources().stream()
                .map(Resource::code)
                .toList();
        if (codes.isEmpty()) {
            return entityManager.createNativeQuery("""
                            update rbac3_resource
                               set status = 'STALE', stale_since = :now,
                                   updated_at = :now, updated_by = 'ci-report'
                             where application_id = :applicationId
                               and source_type = 'CI_REPORT'
                               and status = 'ACTIVE'
                            """)
                    .setParameter("applicationId", applicationId)
                    .setParameter("now", now)
                    .executeUpdate();
        }
        return entityManager.createNativeQuery("""
                        update rbac3_resource
                           set status = 'STALE', stale_since = :now,
                               updated_at = :now, updated_by = 'ci-report'
                         where application_id = :applicationId
                           and source_type = 'CI_REPORT'
                           and resource_code not in (:codes)
                           and status = 'ACTIVE'
                        """)
                .setParameter("applicationId", applicationId)
                .setParameter("codes", codes)
                .setParameter("now", now)
                .executeUpdate();
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException error) {
            throw new IllegalArgumentException("resource report facts are not serializable", error);
        }
    }

    private static String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
