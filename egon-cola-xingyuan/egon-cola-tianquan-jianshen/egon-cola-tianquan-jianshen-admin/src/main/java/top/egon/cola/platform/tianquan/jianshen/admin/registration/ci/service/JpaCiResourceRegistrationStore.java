package top.egon.cola.platform.tianquan.jianshen.admin.registration.ci.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.apibinding.repository.ResourceApiBindingRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.registration.ci.domain.dto.CiResourceRegistrationRequestDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.registration.ci.domain.dto.CiResourceRegistrationRequestDTO.Field;
import top.egon.cola.platform.tianquan.jianshen.admin.registration.ci.domain.dto.CiResourceRegistrationRequestDTO.Resource;
import top.egon.cola.platform.tianquan.jianshen.admin.registration.ci.domain.vo.CiResourceRegistrationResultVO;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.LinkedHashSet;

/** PostgreSQL transaction boundary for global CI registration facts. */
@Repository
public class JpaCiResourceRegistrationStore implements CiResourceRegistrationStore {

    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    private final LongIdGenerator idGenerator;
    private final ResourceApiBindingRepository bindingRepository;

    public JpaCiResourceRegistrationStore(
            EntityManager entityManager,
            ObjectMapper objectMapper,
            LongIdGenerator idGenerator,
            ResourceApiBindingRepository bindingRepository) {
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
        this.idGenerator = idGenerator;
        this.bindingRepository = bindingRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RegistrationHead> findHead(String applicationCode) {
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
        return Optional.of(new RegistrationHead(
                String.valueOf(row[0]),
                String.valueOf(row[1]),
                new CiResourceRegistrationResultVO(0, 0, 0, 0, 0, 0, 0,
                        String.valueOf(row[1]), ((Number) row[2]).longValue()),
                ((Number) row[2]).longValue()));
    }

    @Override
    @Transactional
    public CiResourceRegistrationResultVO replace(
            String applicationCode,
            CiResourceRegistrationRequestDTO request,
            String checksum) {
        Long applicationId = applicationId(applicationCode);
        Instant now = Instant.now();
        int added = 0;
        int updated = 0;
        int pendingMapping = 0;
        Set<ResourceApiBindingRepository.BindingPair> bindings = new LinkedHashSet<>();
        for (Resource resource : request.resources()) {
            boolean exists = resourceExists(applicationId, resource);
            upsertResource(applicationId, resource, request.buildId(), checksum, now);
            if (exists) {
                updated++;
            } else {
                added++;
            }
            if (resource.suggestedPermissionCode() != null) {
                pendingMapping++;
            }
            bindings.addAll(resolveBindings(applicationId, resource));
        }
        for (Field field : request.fields()) {
            boolean exists = fieldExists(applicationId, field);
            upsertField(applicationId, field, request.buildId(), checksum, now);
            if (exists) {
                updated++;
            } else {
                added++;
            }
            pendingMapping++;
        }
        bindingRepository.replaceForApplication(
                applicationId, request.buildId(), checksum, bindings, "ci-registration", now);
        int stale = markStale(applicationId, request, now);
        int applicationUpdated = entityManager.createNativeQuery("""
                        update rbac3_application
                           set ci_report_build_id = :buildId,
                               ci_report_checksum = :checksum,
                               ci_reported_at = :reportedAt,
                               version = version + 1,
                               updated_at = :reportedAt,
                               updated_by = 'ci-registration'
                         where id = :applicationId and version = :expectedVersion
                        """)
                .setParameter("buildId", request.buildId())
                .setParameter("checksum", checksum)
                .setParameter("reportedAt", now)
                .setParameter("applicationId", applicationId)
                .setParameter("expectedVersion", request.expectedApplicationVersion())
                .executeUpdate();
        if (applicationUpdated != 1) {
            throw new top.egon.cola.platform.tianquan.jianshen.core.rule.Rbac3RuleViolation(
                    "RESOURCE_VERSION_CONFLICT");
        }
        CiResourceRegistrationResultVO result = new CiResourceRegistrationResultVO(
                added, updated, stale, 0, pendingMapping, bindings.size(), 0, checksum,
                request.expectedApplicationVersion() + 1L);
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

    private Set<ResourceApiBindingRepository.BindingPair> resolveBindings(
            Long applicationId,
            Resource resource) {
        if (resource.apiResourceCodes().isEmpty()) {
            return Set.of();
        }
        ResourceRef source = resourceRef(applicationId, resource.code());
        if (source == null || (source.type() != top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.domain.enums.ResourceTypeEnum.ROUTE
                && source.type() != top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.domain.enums.ResourceTypeEnum.ACTION)) {
            throw new IllegalArgumentException("binding source must be an active ROUTE or ACTION");
        }
        Set<ResourceApiBindingRepository.BindingPair> pairs = new LinkedHashSet<>();
        for (String apiCode : resource.apiResourceCodes()) {
            ResourceRef api = resourceRef(applicationId, apiCode);
            if (api == null || api.type() != top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.domain.enums.ResourceTypeEnum.API) {
                throw new IllegalArgumentException("binding target must be an API in the same application");
            }
            pairs.add(new ResourceApiBindingRepository.BindingPair(source.id(), api.id()));
        }
        return pairs;
    }

    private ResourceRef resourceRef(Long applicationId, String code) {
        List<?> rows = entityManager.createNativeQuery("""
                        select id, resource_type, status
                          from rbac3_resource
                         where application_id = :applicationId
                           and resource_code = :code
                        """)
                .setParameter("applicationId", applicationId)
                .setParameter("code", code)
                .getResultList();
        if (rows.size() != 1) {
            return null;
        }
        Object[] row = (Object[]) rows.getFirst();
        return new ResourceRef(((Number) row[0]).longValue(),
                top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.domain.enums.ResourceTypeEnum.valueOf(String.valueOf(row[1])),
                String.valueOf(row[2]));
    }

    private record ResourceRef(
            Long id,
            top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.domain.enums.ResourceTypeEnum type,
            String status) {
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
                            resource_name, parent_resource_id, suggested_permission_code,
                            status, source_type, source_build_id, source_checksum,
                            mechanical_facts, display_metadata, version,
                            created_at, created_by, updated_at, updated_by)
                        values (:id, :applicationId, :type, :code, :name,
                                (select id from rbac3_resource where application_id = :applicationId
                                  and resource_code = :parentCode limit 1),
                                :suggestedPermissionCode, 'PENDING_VALIDATION', 'CI_REGISTRATION',
                                :buildId, :checksum, cast(:facts as jsonb),
                                cast(:metadata as jsonb), 0, :now, 'ci-registration', :now, 'ci-registration')
                        on conflict (application_id, resource_type, resource_code)
                        do update set resource_name = excluded.resource_name,
                            suggested_permission_code = excluded.suggested_permission_code,
                            source_type = 'CI_REGISTRATION', source_build_id = excluded.source_build_id,
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
                .setParameter("suggestedPermissionCode", resource.suggestedPermissionCode())
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
                                null, false, false, 'PENDING_VALIDATION', 'CI_REGISTRATION',
                                :buildId, :checksum, :now, 0, :now, 'ci-registration', :now, 'ci-registration')
                        on conflict (application_id, resource_id, field_code)
                        do update set json_path = excluded.json_path,
                            data_type = excluded.data_type,
                            source_type = 'CI_REGISTRATION', source_build_id = excluded.source_build_id,
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
            CiResourceRegistrationRequestDTO request,
            Instant now) {
        List<String> codes = request.resources().stream()
                .map(Resource::code)
                .toList();
        if (codes.isEmpty()) {
            return entityManager.createNativeQuery("""
                            update rbac3_resource
                               set status = 'STALE', stale_since = :now,
                                   updated_at = :now, updated_by = 'ci-registration'
                             where application_id = :applicationId
                               and source_type = 'CI_REGISTRATION'
                               and status = 'ACTIVE'
                            """)
                    .setParameter("applicationId", applicationId)
                    .setParameter("now", now)
                    .executeUpdate();
        }
        return entityManager.createNativeQuery("""
                        update rbac3_resource
                           set status = 'STALE', stale_since = :now,
                               updated_at = :now, updated_by = 'ci-registration'
                         where application_id = :applicationId
                           and source_type = 'CI_REGISTRATION'
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
            throw new IllegalArgumentException("resource registration facts are not serializable", error);
        }
    }

    private static String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
