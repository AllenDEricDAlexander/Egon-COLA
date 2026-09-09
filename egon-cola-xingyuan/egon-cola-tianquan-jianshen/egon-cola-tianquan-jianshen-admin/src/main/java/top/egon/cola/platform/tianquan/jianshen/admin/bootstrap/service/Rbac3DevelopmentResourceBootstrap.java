package top.egon.cola.platform.tianquan.jianshen.admin.bootstrap.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.domain.po.RoleResourceGrantPO;
import top.egon.cola.platform.tianquan.jianshen.admin.bootstrap.domain.Rbac3DevelopmentTopology;
import top.egon.cola.platform.tianquan.jianshen.admin.bootstrap.domain.vo.ApplicationDefinitionVO;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Local-only catalog initializer for the four-platform development topology.
 *
 * <p>The CI registration endpoint intentionally records pending mechanical facts and never
 * confirms an actual permission mapping. A local stack has no release pipeline or operator
 * review step, so this runner provides that explicit development-only confirmation and grants
 * configured, grantable local resources to the built-in administrator roles before role activation.
 * User-created permissions and grouping-only menus are not bootstrap grants.</p>
 */
@Component
@Profile("local")
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(
        prefix = "egon.tianquan-jianshen.development-bootstrap",
        name = "auto-activate-local-admin-roles",
        havingValue = "true")
public class Rbac3DevelopmentResourceBootstrap implements ApplicationRunner {

    /** Classpath resource containing the local Tianquan-Jianshen menu/page/action registry. */
    static final String RESOURCE_DEFINITIONS =
            "bootstrap/tianquan-jianshen-development-resource-definitions.json";

    private static final long BOOTSTRAP_LOCK_KEY = 0x5242414333524553L;
    private static final String ACTOR = "tianquan-jianshen-development-resource-bootstrap";
    private static final String BUILD_ID = "local-tianquan-jianshen-resource-catalog-v1";
    private static final String CHECKSUM = "local-tianquan-jianshen-resource-catalog-v1";

    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    private final LongIdGenerator idGenerator;

    public Rbac3DevelopmentResourceBootstrap(
            EntityManager entityManager,
            ObjectMapper objectMapper,
            LongIdGenerator idGenerator) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    /**
     * Creates the active local catalog before {@link Rbac3DevelopmentBootstrap} runs.
     *
     * @param arguments Spring Boot application arguments; Spring Boot application arguments
     */
    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        acquireLock();
        Instant now = Instant.now();
        Catalog catalog = readCatalog();
        Map<String, Long> applicationIds = applicationIds();
        Map<String, Long> permissionIds = new HashMap<>();

        for (ApplicationDefinitionVO application : Rbac3DevelopmentTopology.applications()) {
            long applicationId = requiredApplicationId(applicationIds, application.applicationCode());
            for (String permissionCode : application.permissions()) {
                permissionIds.put(permissionCode, ensurePermission(
                        applicationId, permissionCode, now));
            }
        }

        long rbac3ApplicationId = requiredApplicationId(applicationIds, "tianquan-jianshen-admin");
        for (DesiredResource resource : catalog.resources()) {
            if (resource.permissionCode() != null) {
                permissionIds.put(resource.permissionCode(), ensurePermission(
                        rbac3ApplicationId, resource.permissionCode(), now));
            }
        }

        Set<Long> bootstrapPermissionIds = Set.copyOf(permissionIds.values());
        List<Object[]> activePermissions = entityManager.createNativeQuery("""
                        select id, application_id, permission_code
                          from rbac3_permission
                         where status = 'ACTIVE'
                        """).getResultList();
        for (Object[] row : activePermissions) {
            long permissionId = ((Number) row[0]).longValue();
            if (!bootstrapPermissionIds.contains(permissionId)) {
                continue;
            }
            long applicationId = ((Number) row[1]).longValue();
            String permissionCode = String.valueOf(row[2]);
            ensureResource(applicationId, new DesiredResource(
                    "API",
                    syntheticResourceCode(applicationId, permissionId),
                    permissionCode,
                    null,
                    permissionCode,
                    null,
                    null,
                    null,
                    null,
                    false,
                    List.of()),
                    permissionId,
                    now);
        }

        for (DesiredResource resource : catalog.resources()) {
            Long permissionId = resource.permissionCode() == null
                    ? null : permissionIds.get(resource.permissionCode());
            ensureResource(rbac3ApplicationId, resource, permissionId, now);
        }
        for (DesiredResource resource : catalog.resources()) {
            if (resource.parentCode() != null) {
                updateParent(rbac3ApplicationId, resource, now);
            }
        }
        for (DesiredResource resource : catalog.resources()) {
            ensureBindings(rbac3ApplicationId, resource, now);
        }
        for (DesiredField field : catalog.fields()) {
            ensureField(rbac3ApplicationId, field, now);
        }
        entityManager.flush();
        ensureLocalAdminResourceGrants(now, bootstrapPermissionIds);
        entityManager.flush();
    }

    private void acquireLock() {
        entityManager.createNativeQuery("select pg_advisory_xact_lock(:lockKey)")
                .setParameter("lockKey", BOOTSTRAP_LOCK_KEY)
                .getSingleResult();
    }

    private Catalog readCatalog() {
        try {
            JsonNode root = objectMapper.readTree(
                    new ClassPathResource(RESOURCE_DEFINITIONS).getInputStream());
            if (!root.isArray()) {
                throw new IllegalStateException("local resource definitions must be an array");
            }
            List<JsonNode> definitions = new ArrayList<>();
            root.forEach(definitions::add);
            Map<String, DesiredResource> resources = new LinkedHashMap<>();
            Map<String, String> apiPermissions = new LinkedHashMap<>();
            List<DesiredField> fields = new ArrayList<>();
            for (JsonNode definition : definitions) {
                String kind = required(definition, "kind");
                String code = required(definition, "code");
                if ("FIELD".equals(kind)) {
                    fields.add(new DesiredField(
                            required(definition, "resourceCode"),
                            required(definition, "fieldCode"),
                            required(definition, "jsonPath"),
                            optional(definition, "dataType", "STRING")));
                    continue;
                }
                String routeCode = optional(definition, "routeCode", null);
                String parentCode = optional(definition, "parentCode", null);
                if ("ACTION".equals(kind) && parentCode == null) {
                    parentCode = routeCode;
                }
                String permissionCode = optional(definition, "suggestedPermissionCode", null);
                JsonNode apiCodes = definition.path("apiResourceCodes");
                List<String> apiResourceCodes = new ArrayList<>();
                if (!apiCodes.isMissingNode() && !apiCodes.isNull()) {
                    if (!apiCodes.isArray()) {
                        throw new IllegalStateException("apiResourceCodes must be an array: " + code);
                    }
                    for (JsonNode apiCode : apiCodes) {
                        String value = apiCode.asText();
                        if (value.isBlank()) {
                            throw new IllegalStateException("apiResourceCodes contains a blank value: " + code);
                        }
                        apiResourceCodes.add(value);
                        String previous = apiPermissions.putIfAbsent(value, permissionCode);
                        if (previous != null && !Objects.equals(previous, permissionCode)) {
                            throw new IllegalStateException(
                                    "local API resource has conflicting permission suggestions: " + value);
                        }
                    }
                }
                DesiredResource resource = new DesiredResource(
                        kind,
                        code,
                        required(definition, "name"),
                        parentCode,
                        permissionCode,
                        optional(definition, "path", null),
                        optional(definition, "componentKey", null),
                        routeCode,
                        integer(definition, "order"),
                        definition.path("hidden").asBoolean(false),
                        List.copyOf(apiResourceCodes));
                resources.put(kind + ':' + code, resource);
            }
            for (Map.Entry<String, String> entry : apiPermissions.entrySet()) {
                resources.putIfAbsent("API:" + entry.getKey(), new DesiredResource(
                        "API",
                        entry.getKey(),
                        "API " + entry.getKey(),
                        null,
                        entry.getValue(),
                        null,
                        null,
                        null,
                        null,
                        false,
                        List.of()));
            }
            return new Catalog(List.copyOf(resources.values()), List.copyOf(fields));
        } catch (IOException error) {
            throw new IllegalStateException(
                    "local Tianquan-Jianshen resource definitions cannot be read", error);
        }
    }

    private Map<String, Long> applicationIds() {
        Map<String, Long> result = new HashMap<>();
        for (Object value : entityManager.createNativeQuery("""
                        select id, application_code
                          from rbac3_application
                        """).getResultList()) {
            Object[] row = (Object[]) value;
            result.put(String.valueOf(row[1]), ((Number) row[0]).longValue());
        }
        return result;
    }

    private long requiredApplicationId(Map<String, Long> applicationIds, String code) {
        Long applicationId = applicationIds.get(code);
        if (applicationId == null) {
            throw new IllegalStateException("local Tianquan-Jianshen application is missing: " + code);
        }
        return applicationId;
    }

    private long ensurePermission(long applicationId, String permissionCode, Instant now) {
        List<?> rows = entityManager.createNativeQuery("""
                        select id, application_id, status
                          from rbac3_permission
                         where permission_code = :permissionCode
                        """)
                .setParameter("permissionCode", permissionCode)
                .getResultList();
        if (!rows.isEmpty()) {
            Object[] row = (Object[]) rows.getFirst();
            long existingApplicationId = ((Number) row[1]).longValue();
            if (existingApplicationId != applicationId) {
                throw new IllegalStateException(
                        "local permission is owned by another application: " + permissionCode);
            }
            if (!"ACTIVE".equals(String.valueOf(row[2]))) {
                entityManager.createNativeQuery("""
                                update rbac3_permission
                                   set status = 'ACTIVE', version = version + 1,
                                       updated_at = :now, updated_by = :actor
                                 where id = :id
                                """)
                        .setParameter("now", now)
                        .setParameter("actor", ACTOR)
                        .setParameter("id", ((Number) row[0]).longValue())
                        .executeUpdate();
            }
            return ((Number) row[0]).longValue();
        }
        long id = idGenerator.nextLongId();
        entityManager.createNativeQuery("""
                        insert into rbac3_permission (
                            id, application_id, permission_code, permission_name,
                            risk_level, status, description, source_type, source_build_id,
                            source_checksum, version, created_at, created_by, updated_at, updated_by)
                        values (:id, :applicationId, :permissionCode, :permissionCode,
                                :riskLevel, 'ACTIVE', :description, 'MANUAL', :buildId,
                                :checksum, 0, :now, :actor, :now, :actor)
                        """)
                .setParameter("id", id)
                .setParameter("applicationId", applicationId)
                .setParameter("permissionCode", permissionCode)
                .setParameter("riskLevel", risk(permissionCode))
                .setParameter("description", "Local development resource authorization capability")
                .setParameter("buildId", BUILD_ID)
                .setParameter("checksum", CHECKSUM)
                .setParameter("now", now)
                .setParameter("actor", ACTOR)
                .executeUpdate();
        return id;
    }

    private void ensureResource(
            long applicationId,
            DesiredResource resource,
            Long permissionId,
            Instant now) {
        ObjectNode facts = objectMapper.createObjectNode()
                .put("hidden", resource.hidden());
        put(facts, "path", resource.path());
        put(facts, "componentKey", resource.componentKey());
        put(facts, "routeCode", resource.routeCode());
        if (resource.order() == null) {
            facts.putNull("order");
        } else {
            facts.put("order", resource.order());
        }
        ObjectNode metadata = objectMapper.createObjectNode()
                .put("name", resource.name());
        entityManager.createNativeQuery("""
                        insert into rbac3_resource (
                            id, application_id, resource_type, resource_code, resource_name,
                            parent_resource_id, required_permission_id, suggested_permission_code,
                            status, source_type,
                            source_build_id, source_checksum, mechanical_facts, display_metadata,
                            version, created_at, created_by, updated_at, updated_by)
                        values (:id, :applicationId, :resourceType, :resourceCode, :resourceName,
                                null, :permissionId, :suggestedPermissionCode, 'ACTIVE', 'MANUAL',
                                :buildId, :checksum,
                                cast(:facts as jsonb), cast(:metadata as jsonb), 0,
                                :now, :actor, :now, :actor)
                        on conflict (application_id, resource_type, resource_code)
                        do update set resource_name = excluded.resource_name,
                            suggested_permission_code = excluded.suggested_permission_code,
                            mechanical_facts = excluded.mechanical_facts,
                            display_metadata = excluded.display_metadata,
                            required_permission_id = coalesce(
                                rbac3_resource.required_permission_id,
                                excluded.required_permission_id),
                            status = case
                                when rbac3_resource.status in ('PENDING_VALIDATION', 'STALE')
                                then 'ACTIVE' else rbac3_resource.status end,
                            source_type = 'MANUAL', source_build_id = excluded.source_build_id,
                            source_checksum = excluded.source_checksum, stale_since = null,
                            updated_at = excluded.updated_at, updated_by = excluded.updated_by
                        """)
                .setParameter("id", idGenerator.nextLongId())
                .setParameter("applicationId", applicationId)
                .setParameter("resourceType", resource.type())
                .setParameter("resourceCode", resource.code())
                .setParameter("resourceName", resource.name())
                .setParameter("permissionId", permissionId)
                .setParameter("suggestedPermissionCode", resource.permissionCode())
                .setParameter("buildId", BUILD_ID)
                .setParameter("checksum", CHECKSUM)
                .setParameter("facts", facts.toString())
                .setParameter("metadata", metadata.toString())
                .setParameter("now", now)
                .setParameter("actor", ACTOR)
                .executeUpdate();
    }

    private void updateParent(long applicationId, DesiredResource resource, Instant now) {
        long parentId = ((Number) entityManager.createNativeQuery("""
                        select id
                          from rbac3_resource
                         where application_id = :applicationId
                           and resource_code = :resourceCode
                         order by case resource_type when 'ROUTE' then 0
                                  when 'MENU' then 1 else 2 end
                         limit 1
                        """)
                .setParameter("applicationId", applicationId)
                .setParameter("resourceCode", resource.parentCode())
                .getSingleResult()).longValue();
        entityManager.createNativeQuery("""
                        update rbac3_resource
                           set parent_resource_id = :parentId,
                               updated_at = :now, updated_by = :actor
                         where application_id = :applicationId
                           and resource_type = :resourceType
                           and resource_code = :resourceCode
                        """)
                .setParameter("parentId", parentId)
                .setParameter("now", now)
                .setParameter("actor", ACTOR)
                .setParameter("applicationId", applicationId)
                .setParameter("resourceType", resource.type())
                .setParameter("resourceCode", resource.code())
                .executeUpdate();
    }

    private void ensureBindings(long applicationId, DesiredResource resource, Instant now) {
        if (resource.apiResourceCodes().isEmpty()) {
            return;
        }
        for (String apiCode : resource.apiResourceCodes()) {
            long sourceId = resourceId(applicationId, resource.type(), resource.code());
            long apiId = resourceId(applicationId, "API", apiCode);
            entityManager.createNativeQuery("""
                            insert into rbac3_resource_api_binding (
                                id, application_id, source_resource_id, api_resource_id,
                                source_build_id, source_checksum, status, version,
                                created_at, created_by, updated_at, updated_by)
                            values (:id, :applicationId, :sourceId, :apiId, :buildId, :checksum,
                                    'ACTIVE', 0, :now, :actor, :now, :actor)
                            on conflict (source_resource_id, api_resource_id)
                            do update set source_build_id = excluded.source_build_id,
                                source_checksum = excluded.source_checksum, status = 'ACTIVE',
                                updated_at = excluded.updated_at, updated_by = excluded.updated_by
                            """)
                    .setParameter("id", idGenerator.nextLongId())
                    .setParameter("applicationId", applicationId)
                    .setParameter("sourceId", sourceId)
                    .setParameter("apiId", apiId)
                    .setParameter("buildId", BUILD_ID)
                    .setParameter("checksum", CHECKSUM)
                    .setParameter("now", now)
                    .setParameter("actor", ACTOR)
                    .executeUpdate();
        }
    }

    private void ensureField(long applicationId, DesiredField field, Instant now) {
        long resourceId = resourceId(applicationId, "ROUTE", field.resourceCode());
        entityManager.createNativeQuery("""
                        insert into rbac3_field_definition (
                            id, application_id, resource_id, field_code, json_path, data_type,
                            sensitivity, default_access, masking_strategy, writable, exportable,
                            status, source_type, source_build_id, source_checksum, version,
                            created_at, created_by, updated_at, updated_by)
                        values (:id, :applicationId, :resourceId, :fieldCode, :jsonPath, :dataType,
                                'NORMAL', 'NONE', null, false, false, 'ACTIVE', 'MANUAL',
                                :buildId, :checksum, 0, :now, :actor, :now, :actor)
                        on conflict (application_id, resource_id, field_code)
                        do update set json_path = excluded.json_path,
                            data_type = excluded.data_type, status = 'ACTIVE',
                            source_type = 'MANUAL', source_build_id = excluded.source_build_id,
                            source_checksum = excluded.source_checksum,
                            updated_at = excluded.updated_at, updated_by = excluded.updated_by
                        """)
                .setParameter("id", idGenerator.nextLongId())
                .setParameter("applicationId", applicationId)
                .setParameter("resourceId", resourceId)
                .setParameter("fieldCode", field.fieldCode())
                .setParameter("jsonPath", field.jsonPath())
                .setParameter("dataType", field.dataType())
                .setParameter("buildId", BUILD_ID)
                .setParameter("checksum", CHECKSUM)
                .setParameter("now", now)
                .setParameter("actor", ACTOR)
                .executeUpdate();
    }

    private void ensureLocalAdminResourceGrants(Instant now, Set<Long> permissionIds) {
        Map<Long, List<Long>> resourceIdsByApplication = new HashMap<>();
        for (Object value : entityManager.createNativeQuery("""
                        select r.id, r.application_id
                          from rbac3_resource r
                          join rbac3_permission p
                            on p.id = r.required_permission_id
                           and p.application_id = r.application_id
                         where r.status = 'ACTIVE' and p.status = 'ACTIVE'
                           and r.resource_type in ('ROUTE', 'ACTION', 'API')
                           and p.id in (:permissionIds)
                           and r.source_build_id = :buildId
                         order by r.application_id, r.id
                        """)
                .setParameter("permissionIds", permissionIds)
                .setParameter("buildId", BUILD_ID)
                .getResultList()) {
            Object[] row = (Object[]) value;
            long resourceId = ((Number) row[0]).longValue();
            long applicationId = ((Number) row[1]).longValue();
            resourceIdsByApplication
                    .computeIfAbsent(applicationId, ignored -> new ArrayList<>())
                    .add(resourceId);
        }

        for (Object value : entityManager.createNativeQuery("""
                        select id, tenant_id, application_id
                          from rbac3_role
                         where status = 'ACTIVE'
                           and role_code like '%_LOCAL_ADMIN'
                         order by application_id, tenant_id, id
                        """).getResultList()) {
            Object[] row = (Object[]) value;
            long roleId = ((Number) row[0]).longValue();
            long tenantId = ((Number) row[1]).longValue();
            long applicationId = ((Number) row[2]).longValue();
            for (long resourceId : resourceIdsByApplication.getOrDefault(
                    applicationId, List.of())) {
                if (hasActiveGrant(tenantId, roleId, resourceId, now)) {
                    continue;
                }
                entityManager.persist(new RoleResourceGrantPO(
                        idGenerator.nextLongId(), tenantId, applicationId,
                        roleId, resourceId, now, null, ACTOR, now));
            }
        }
    }

    private boolean hasActiveGrant(
            long tenantId,
            long roleId,
            long resourceId,
            Instant now) {
        return !entityManager.createNativeQuery("""
                        select 1
                          from rbac3_role_resource_grant
                         where tenant_id = :tenantId
                           and role_id = :roleId
                           and resource_id = :resourceId
                           and status = 'ACTIVE'
                           and valid_from <= :now
                           and (valid_to is null or valid_to > :now)
                         limit 1
                        """)
                .setParameter("tenantId", tenantId)
                .setParameter("roleId", roleId)
                .setParameter("resourceId", resourceId)
                .setParameter("now", now)
                .getResultList()
                .isEmpty();
    }

    private long resourceId(long applicationId, String type, String code) {
        return ((Number) entityManager.createNativeQuery("""
                        select id from rbac3_resource
                         where application_id = :applicationId
                           and resource_type = :resourceType
                           and resource_code = :resourceCode
                        """)
                .setParameter("applicationId", applicationId)
                .setParameter("resourceType", type)
                .setParameter("resourceCode", code)
                .getSingleResult()).longValue();
    }

    private static String syntheticResourceCode(long applicationId, long permissionId) {
        return "__local.permission." + applicationId + '.' + permissionId;
    }

    private static String risk(String permissionCode) {
        if (permissionCode.endsWith(":read") || "TIANSHU_READ".equals(permissionCode)) {
            return "MEDIUM";
        }
        if (permissionCode.endsWith(":manage") || permissionCode.endsWith(":admin")
                || permissionCode.endsWith(":activate") || permissionCode.endsWith(":revoke")) {
            return "CRITICAL";
        }
        return "HIGH";
    }

    private static String required(JsonNode node, String field) {
        String value = optional(node, field, null);
        if (value == null) {
            throw new IllegalStateException("local resource definition requires " + field);
        }
        return value;
    }

    private static String optional(JsonNode node, String field, String fallback) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? fallback : value.asText();
    }

    private static Integer integer(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asInt();
    }

    private static void put(ObjectNode node, String field, String value) {
        if (value == null) {
            node.putNull(field);
        } else {
            node.put(field, value);
        }
    }

    private record Catalog(List<DesiredResource> resources, List<DesiredField> fields) {
        private Catalog {
            resources = List.copyOf(resources);
            fields = List.copyOf(fields);
        }
    }

    private record DesiredResource(
            String type,
            String code,
            String name,
            String parentCode,
            String permissionCode,
            String path,
            String componentKey,
            String routeCode,
            Integer order,
            boolean hidden,
            List<String> apiResourceCodes) {
        private DesiredResource(
                String type,
                String code,
                String name,
                String parentCode,
                String permissionCode,
                String path,
                String componentKey,
                String routeCode,
                Integer order,
                boolean hidden,
                List<String> apiResourceCodes) {
            this.type = type;
            this.code = code;
            this.name = name;
            this.parentCode = parentCode;
            this.permissionCode = permissionCode;
            this.path = path;
            this.componentKey = componentKey;
            this.routeCode = routeCode;
            this.order = order;
            this.hidden = hidden;
            this.apiResourceCodes = List.copyOf(apiResourceCodes);
        }
    }

    private record DesiredField(
            String resourceCode,
            String fieldCode,
            String jsonPath,
            String dataType) {
    }
}
