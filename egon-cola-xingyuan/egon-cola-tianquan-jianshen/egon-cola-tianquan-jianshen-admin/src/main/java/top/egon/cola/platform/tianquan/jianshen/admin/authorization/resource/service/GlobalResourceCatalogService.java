package top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.service;

import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.Accessors;
import org.springframework.beans.factory.annotation.Autowired;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.permission.domain.dto.CreatePermissionRequestDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.permission.service.PermissionCatalogService;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.business.service.DdcCatalogGateway;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.domain.command.DeploymentCatalogRegistrationCommand.PermissionDefinitionBO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.domain.command.DeploymentCatalogRegistrationCommand.ResourceDefinitionBO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.domain.command.DeploymentCatalogRegistrationCommand.FieldDefinitionBO;
import org.springframework.validation.annotation.Validated;
import org.springframework.beans.factory.annotation.Qualifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.domain.command.DeploymentCatalogRegistrationCommand;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.domain.vo.DeploymentCatalogRegistrationResultVO;
import top.egon.cola.platform.tianquan.jianshen.core.rule.Rbac3RuleViolation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.application.domain.vo.ApplicationVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.domain.vo.ArchiveResultVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.domain.vo.ResourceVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.field.domain.dto.ChangeFieldDefinitionStatusRequestDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.field.domain.dto.CreateFieldDefinitionRequestDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.field.domain.vo.FieldDefinitionVO;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.platform.tianquan.jianshen.admin.shared.domain.DatabaseClock;

import java.time.Instant;
import java.util.List;

/** Global application/resource catalog queries and manual resource lifecycle operations. */
@Service("globalResourceCatalogService")
@Validated
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class GlobalResourceCatalogService {

    @Qualifier("jpaSharedEM_entityManagerFactory")
    private final EntityManager entityManager;
    @Qualifier("jpaDatabaseClock")
    private final DatabaseClock clock;

    @Qualifier("permissionCatalogService")
    private final PermissionCatalogService permissionCatalog;
    @Qualifier("ddcCatalogGateway")
    private final DdcCatalogGateway deploymentCatalog;
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;
    @Qualifier("defaultValidator")
    private final Validator validator;
    @Qualifier("deploymentPermissionRegistrationConverterImpl")
    private final PermissionRegistrationConverter permissionConverter;

    /** Preserves existing query/manual CRUD callers; deployment registration requires the configured ports. */
    public GlobalResourceCatalogService(EntityManager entityManager, DatabaseClock clock) {
        this(entityManager, clock, null, null, null, null, null);
    }

    /** Registers mechanical facts atomically; review, mappings and role grants remain separate commands. */
    @Transactional
    @Validated(DeploymentCatalogRegistrationCommand.Registration.class)
    public DeploymentCatalogRegistrationResultVO registerDeploymentCatalog(
            @Valid DeploymentCatalogRegistrationCommand command, String actorId) {
        if (permissionCatalog == null || deploymentCatalog == null || objectMapper == null || validator == null || permissionConverter == null) {
            throw new Rbac3RuleViolation("RESOURCE_CATALOG_REGISTRATION_UNAVAILABLE");
        }
        new ValidationUtils(validator).validate(command, DeploymentCatalogRegistrationCommand.Registration.class);
        if (actorId == null || actorId.isBlank() || actorId.length() > 128 || !actorId.equals(actorId.strip())) {
            throw new IllegalArgumentException("Verified deployment actor is required");
        }
        validateNamespace(command);
        String digest = catalogChecksum(command);
        if (!digest.equals(command.getChecksum())) { throw new IllegalArgumentException("Deployment catalog checksum mismatch"); }
        List<?> rows = entityManager.createNativeQuery("""
                select id, application_code, status, version, ddc_business_id, ddc_application_id,
                       ci_report_build_id, ci_report_checksum
                  from rbac3_application where application_code = :code for update
                """).setParameter("code", command.getApplicationCode()).getResultList();
        if (rows.size() != 1) { throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND"); }
        Object[] app = (Object[]) rows.getFirst(); long applicationId = ((Number) app[0]).longValue(); long version = ((Number) app[3]).longValue();
        if (!"ACTIVE".equals(app[2]) || !command.getApplicationCode().equals(app[1])) { throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND"); }
        requireDeploymentOwner(command, String.valueOf(app[4]), String.valueOf(app[5]));
        if (command.getBuildId().equals(app[6])) {
            if (!digest.equals(app[7])) { throw new Rbac3RuleViolation("RESOURCE_VERSION_CONFLICT"); }
            return registrationReceipt(command, applicationId, version);
        }
        if (version != command.getExpectedApplicationVersion()) { throw new Rbac3RuleViolation("RESOURCE_VERSION_CONFLICT"); }
        // Check every existing definition before issuing any insert; a foreign or changed owner cannot partially register.
        for (PermissionDefinitionBO definition : command.getPermissions()) { requireCompatiblePermission(applicationId, definition); }
        for (ResourceDefinitionBO definition : command.getResources()) { requireCompatibleResource(applicationId, definition); }
        Instant now = clock.transactionNow();
        for (PermissionDefinitionBO definition : command.getPermissions()) {
            if (permissionRows(definition.getPermissionCode()).isEmpty()) {
                var projection = PermissionRegistrationBO.builder().applicationId(Long.toString(applicationId)).definition(definition).build();
                new ValidationUtils(validator).validate(projection, DeploymentCatalogRegistrationCommand.Registration.class);
                var create = new ValidationUtils(validator).validate(permissionConverter.toTarget(projection));
                permissionCatalog.create(create, actorId);
            }
        }
        for (ResourceDefinitionBO definition : command.getResources()) { registerMechanicalResource(applicationId, definition, command, actorId, now); }
        int updated = entityManager.createNativeQuery("""
                update rbac3_application set ci_report_build_id = :buildId, ci_report_checksum = :checksum,
                       ci_reported_at = :now, version = version + 1, updated_at = :now, updated_by = :actor
                 where id = :id and version = :version and status = 'ACTIVE'
                """).setParameter("buildId", command.getBuildId()).setParameter("checksum", digest).setParameter("now", now)
                .setParameter("actor", actorId).setParameter("id", applicationId).setParameter("version", version).executeUpdate();
        if (updated != 1) { throw new Rbac3RuleViolation("RESOURCE_VERSION_CONFLICT"); }
        entityManager.flush();
        log.info("Deployment catalog registered pending review; applicationId={}, permissionCount={}, resourceCount={}, fieldCount={}",
                applicationId, command.getPermissions().size(), command.getResources().size(), command.getFields().size());
        return registrationReceipt(command, applicationId, Math.addExact(version, 1));
    }

    private void validateNamespace(DeploymentCatalogRegistrationCommand command) {
        String code = command.getApplicationCode();
        if (command.getPermissions().stream().anyMatch(definition -> !definition.getPermissionCode().startsWith(code + ":"))
                || command.getResources().stream().anyMatch(definition -> !definition.getResourceCode().startsWith(code + ".")
                        || definition.getSuggestedPermissionCode() != null && !definition.getSuggestedPermissionCode().startsWith(code + ":"))) {
            throw new IllegalArgumentException("Deployment catalog definitions have a foreign application namespace");
        }
    }

    private void requireDeploymentOwner(DeploymentCatalogRegistrationCommand command, String businessId, String applicationId) {
        var business = deploymentCatalog.findBusiness(businessId).orElseThrow(() -> new Rbac3RuleViolation("RESOURCE_NOT_FOUND"));
        var application = deploymentCatalog.findApplication(applicationId).orElseThrow(() -> new Rbac3RuleViolation("RESOURCE_NOT_FOUND"));
        if (!business.enabled() || !application.businessEnabled() || !application.applicationEnabled()
                || !businessId.equals(business.ddcBusinessId()) || !businessId.equals(application.ddcBusinessId())
                || !applicationId.equals(application.ddcApplicationId()) || !command.getBusinessCode().equals(business.bizCode())
                || !command.getBusinessCode().equals(application.bizCode()) || !command.getApplicationCode().equals(application.appCode())) {
            throw new Rbac3RuleViolation("ROLE_APPLICATION_MISMATCH");
        }
    }

    private String catalogChecksum(DeploymentCatalogRegistrationCommand command) {
        var payload = new java.util.TreeMap<String, Object>();
        payload.put("businessCode", command.getBusinessCode()); payload.put("applicationCode", command.getApplicationCode()); payload.put("buildId", command.getBuildId());
        payload.put("permissions", command.getPermissions().stream().sorted(java.util.Comparator.comparing(PermissionDefinitionBO::getPermissionCode)).toList());
        payload.put("resources", command.getResources().stream().sorted(java.util.Comparator.comparing(ResourceDefinitionBO::getResourceCode)).toList());
        payload.put("fields", command.getFields().stream().sorted(java.util.Comparator.comparing(FieldDefinitionBO::getResourceCode).thenComparing(FieldDefinitionBO::getFieldCode)).toList());
        try {
            // Digest serialization uses the host's Jackson modules and an explicit stable key order, not model conversion.
            ObjectMapper canonical = objectMapper.copy().configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                    .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(canonical.writeValueAsBytes(payload)));
        } catch (com.fasterxml.jackson.core.JsonProcessingException | java.security.NoSuchAlgorithmException failure) {
            throw new IllegalStateException("Deployment catalog checksum is unavailable", failure);
        }
    }

    private List<?> permissionRows(String code) {
        return entityManager.createNativeQuery("""
                select application_id, permission_name, risk_level, description, status
                  from rbac3_permission where permission_code = :code
                """).setParameter("code", code).getResultList();
    }

    private void requireCompatiblePermission(long applicationId, PermissionDefinitionBO definition) {
        List<?> rows = permissionRows(definition.getPermissionCode());
        if (rows.isEmpty()) { return; }
        if (rows.size() != 1) { throw new Rbac3RuleViolation("RESOURCE_VERSION_CONFLICT"); }
        Object[] row = (Object[]) rows.getFirst();
        if (((Number) row[0]).longValue() != applicationId || !definition.getPermissionName().equals(row[1])
                || !definition.getRiskLevel().equals(row[2]) || !java.util.Objects.equals(definition.getDescription(), row[3])) {
            throw new Rbac3RuleViolation("ROLE_APPLICATION_MISMATCH");
        }
    }

    private List<?> resourceRows(long applicationId, String code) {
        return entityManager.createNativeQuery("""
                select id, resource_name, status, version, mechanical_facts
                  from rbac3_resource where application_id = :applicationId and resource_type = 'API' and resource_code = :code
                """).setParameter("applicationId", applicationId).setParameter("code", code).getResultList();
    }

    private void requireCompatibleResource(long applicationId, ResourceDefinitionBO definition) {
        List<?> rows = resourceRows(applicationId, definition.getResourceCode());
        if (rows.isEmpty()) { return; }
        if (rows.size() != 1) { throw new Rbac3RuleViolation("RESOURCE_VERSION_CONFLICT"); }
        Object[] row = (Object[]) rows.getFirst();
        try {
            var facts = objectMapper.readTree(String.valueOf(row[4]));
            for (String key : List.of("transport", "method", "path", "operationId", "fullMethodName")) {
                String expected = switch (key) { case "transport" -> definition.getTransport(); case "method" -> definition.getMethod();
                    case "path" -> definition.getPath(); case "operationId" -> definition.getOperationId(); default -> definition.getFullMethodName(); };
                String current = facts.hasNonNull(key) ? facts.get(key).asText() : null;
                if (key.equals("transport") && current == null && !facts.hasNonNull("fullMethodName")) { current = "REST"; }
                if (!java.util.Objects.equals(expected, current)) { throw new Rbac3RuleViolation("RESOURCE_VERSION_CONFLICT"); }
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException failure) { throw new IllegalStateException("Existing catalog facts are unavailable", failure); }
    }

    private void registerMechanicalResource(long applicationId, ResourceDefinitionBO definition, DeploymentCatalogRegistrationCommand command, String actor, Instant now) {
        var facts = new java.util.TreeMap<String, Object>(); facts.put("transport", definition.getTransport()); facts.put("method", definition.getMethod());
        facts.put("path", definition.getPath()); facts.put("operationId", definition.getOperationId()); facts.put("fullMethodName", definition.getFullMethodName());
        facts.put("declaredFields", command.getFields().stream().filter(field -> field.getResourceCode().equals(definition.getResourceCode())).sorted(java.util.Comparator.comparing(FieldDefinitionBO::getFieldCode)).toList());
        List<?> rows = resourceRows(applicationId, definition.getResourceCode());
        String json;
        try {
            if (!rows.isEmpty()) {
                // Refresh only the explicitly reported keys; retain other owner-controlled mechanical metadata.
                var existing = objectMapper.readValue(String.valueOf(((Object[]) rows.getFirst())[4]),
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.TreeMap<String, Object>>() { });
                existing.putAll(facts); facts = existing;
            }
            json = objectMapper.writeValueAsString(facts);
        } catch (com.fasterxml.jackson.core.JsonProcessingException failure) { throw new IllegalStateException("Mechanical catalog facts are unavailable", failure); }
        if (rows.isEmpty()) {
            int written = entityManager.createNativeQuery("""
                    insert into rbac3_resource (id, application_id, resource_type, resource_code, resource_name, status,
                           mechanical_facts, display_metadata, source_type, source_build_id, source_checksum, suggested_permission_code,
                           ci_reported_at, version, created_at, created_by, updated_at, updated_by)
                    values (:id, :applicationId, 'API', :code, :name, 'ACTIVE', cast(:facts as jsonb), '{}',
                            'CI_REGISTRATION', :buildId, :checksum, :suggested, :now, 0, :now, :actor, :now, :actor)
                    """).setParameter("id", SnowflakeIdGenerator.nextLongId()).setParameter("applicationId", applicationId)
                    .setParameter("code", definition.getResourceCode()).setParameter("name", definition.getName()).setParameter("facts", json)
                    .setParameter("buildId", command.getBuildId()).setParameter("checksum", command.getChecksum()).setParameter("suggested", definition.getSuggestedPermissionCode())
                    .setParameter("now", now).setParameter("actor", actor).executeUpdate();
            if (written != 1) { throw new Rbac3RuleViolation("RESOURCE_VERSION_CONFLICT"); }
        } else {
            Object[] row = (Object[]) rows.getFirst();
            int updated = entityManager.createNativeQuery("""
                    update rbac3_resource set mechanical_facts = cast(:facts as jsonb), source_build_id = :buildId,
                           source_checksum = :checksum, ci_reported_at = :now, version = version + 1, updated_at = :now, updated_by = :actor
                     where id = :id and application_id = :applicationId and version = :version
                    """).setParameter("facts", json).setParameter("buildId", command.getBuildId()).setParameter("checksum", command.getChecksum())
                    .setParameter("now", now).setParameter("actor", actor).setParameter("id", row[0]).setParameter("applicationId", applicationId)
                    .setParameter("version", row[3]).executeUpdate();
            if (updated != 1) { throw new Rbac3RuleViolation("RESOURCE_VERSION_CONFLICT"); }
        }
    }

    private DeploymentCatalogRegistrationResultVO registrationReceipt(DeploymentCatalogRegistrationCommand command, long applicationId, long version) {
        return new DeploymentCatalogRegistrationResultVO(Long.toString(applicationId), command.getApplicationCode(), command.getBuildId(), command.getChecksum(), version,
                command.getPermissions().size(), command.getResources().size(), command.getFields().size(), "REGISTERED_PENDING_REVIEW");
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
    public static class PermissionRegistrationBO {
        @NotBlank(groups = DeploymentCatalogRegistrationCommand.Registration.class)
        @Pattern(regexp = "[1-9][0-9]{0,18}", groups = DeploymentCatalogRegistrationCommand.Registration.class) private String applicationId;
        @NotNull(groups = DeploymentCatalogRegistrationCommand.Registration.class) @Valid private PermissionDefinitionBO definition;
    }

    @Mapper(componentModel = MappingConstants.ComponentModel.SPRING, implementationName = "DeploymentPermissionRegistrationConverterImpl")
    public interface PermissionRegistrationConverter extends BaseForwardConverter<PermissionRegistrationBO, CreatePermissionRequestDTO> {
        @Override
        @Mapping(target = "permissionCode", source = "definition.permissionCode")
        @Mapping(target = "permissionName", source = "definition.permissionName")
        @Mapping(target = "riskLevel", source = "definition.riskLevel")
        @Mapping(target = "description", source = "definition.description")
        CreatePermissionRequestDTO toTarget(PermissionRegistrationBO source);
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
        long id = SnowflakeIdGenerator.nextLongId();
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
                row[6] == null ? "UNCONFIGURED" : "CONFIGURED",
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
