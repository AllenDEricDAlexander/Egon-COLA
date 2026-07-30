package top.egon.cola.platform.rbac3.admin.resource.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.application.port.AuthorizationEventPort;
import top.egon.cola.platform.rbac3.admin.application.port.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.identity.domain.TenantEntity;
import top.egon.cola.platform.rbac3.admin.resource.application.ApplicationResourceFacade;
import top.egon.cola.platform.rbac3.admin.resource.application.ManifestFacade;
import top.egon.cola.platform.rbac3.admin.resource.domain.ApplicationEntity;
import top.egon.cola.platform.rbac3.admin.resource.domain.FieldDefinitionEntity;
import top.egon.cola.platform.rbac3.admin.resource.domain.PermissionEntity;
import top.egon.cola.platform.rbac3.admin.resource.domain.PermissionResourceEntity;
import top.egon.cola.platform.rbac3.admin.resource.domain.ResourceEntity;
import top.egon.cola.platform.rbac3.admin.resource.domain.ResourceManifestEntity;
import top.egon.cola.platform.rbac3.contract.manifest.ManifestResource;
import top.egon.cola.platform.rbac3.contract.manifest.ResourceManifest;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ResourceManifestRepository implements
        ManifestFacade.ManifestStore,
        ApplicationResourceFacade.Store {

    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    private final LongIdGenerator idGenerator;
    private final DatabaseClock databaseClock;
    private final AuthorizationEventPort eventPort;

    public ResourceManifestRepository(
            EntityManager entityManager,
            ObjectMapper objectMapper,
            LongIdGenerator idGenerator,
            DatabaseClock databaseClock,
            AuthorizationEventPort eventPort) {
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
        this.idGenerator = idGenerator;
        this.databaseClock = databaseClock;
        this.eventPort = eventPort;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ManifestFacade.StoredManifest> findByBuild(
            String tenantId,
            String applicationId,
            String artifactVersion,
            String buildId) {
        return entityManager.createQuery("""
                        select m from ResourceManifestEntity m
                         where m.tenantId = :tenantId
                           and m.applicationId = :applicationId
                           and m.artifactVersion = :artifactVersion
                           and m.buildId = :buildId
                        """, ResourceManifestEntity.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("applicationId", Long.valueOf(applicationId))
                .setParameter("artifactVersion", artifactVersion)
                .setParameter("buildId", buildId)
                .getResultStream()
                .findFirst()
                .map(this::toStoredManifest);
    }

    @Override
    @Transactional
    public void insert(ManifestFacade.StoredManifest manifest) {
        Instant now = databaseClock.transactionNow();
        Map<String, Object> payload = objectMapper.convertValue(
                manifest.manifest(), new TypeReference<>() { });
        ResourceManifestEntity entity = new ResourceManifestEntity(
                Long.valueOf(manifest.manifestId()),
                Long.valueOf(manifest.tenantId()),
                Long.valueOf(manifest.applicationId()),
                Integer.parseInt(manifest.manifest().schemaVersion()),
                manifest.artifactVersion(),
                manifest.buildId(),
                manifest.manifestVersion(),
                manifest.checksum(),
                manifest.definitionSetId(),
                payload,
                "gateway-report",
                now);
        entityManager.persist(entity);
        materializeResources(manifest, now);
    }

    @Override
    @Transactional
    public ActivationMutation activate(
            String tenantId,
            String applicationId,
            String manifestId,
            long expectedApplicationVersion,
            long expectedCurrentManifestVersion,
            String expectedDefinitionSetId,
            String actorId,
            String idempotencyKey,
            String reason,
            Instant ignoredNow) {
        Instant now = databaseClock.transactionNow();
        ApplicationEntity application = entityManager.find(
                ApplicationEntity.class,
                Long.valueOf(applicationId),
                LockModeType.PESSIMISTIC_WRITE);
        ResourceManifestEntity manifest = entityManager.find(
                ResourceManifestEntity.class,
                Long.valueOf(manifestId),
                LockModeType.PESSIMISTIC_WRITE);
        TenantEntity tenant = entityManager.find(
                TenantEntity.class,
                Long.valueOf(tenantId),
                LockModeType.PESSIMISTIC_WRITE);
        if (application == null || manifest == null || tenant == null
                || !application.getTenantId().equals(Long.valueOf(tenantId))
                || !manifest.getTenantId().equals(Long.valueOf(tenantId))
                || !manifest.getApplicationId().equals(Long.valueOf(applicationId))) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        if (application.getVersion() != expectedApplicationVersion) {
            throw new Rbac3RuleViolation("RESOURCE_VERSION_CONFLICT");
        }
        long currentManifestVersion = application.getCurrentManifestVersion() == null
                ? 0L : application.getCurrentManifestVersion();
        if (currentManifestVersion != expectedCurrentManifestVersion) {
            throw new Rbac3RuleViolation("RESOURCE_VERSION_CONFLICT");
        }
        if (!manifest.getDefinitionSetId().equals(expectedDefinitionSetId)) {
            throw new Rbac3RuleViolation("RESOURCE_MANIFEST_CONFLICT");
        }
        entityManager.createQuery("""
                        select m from ResourceManifestEntity m
                         where m.tenantId = :tenantId
                           and m.applicationId = :applicationId
                           and m.status = :status
                        """, ResourceManifestEntity.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("applicationId", Long.valueOf(applicationId))
                .setParameter("status", ResourceManifestEntity.Status.ACTIVE)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList()
                .forEach(current -> current.supersede(actorId, now));
        manifest.activate(actorId, now);
        application.activateManifest(
                manifest.getId(), manifest.getManifestVersion(), actorId, now);
        tenant.incrementPolicyVersion(actorId, now);
        activateResourceProjection(
                Long.valueOf(tenantId), Long.valueOf(applicationId), manifest.getId(), actorId, now);
        String propagationId = eventPort.enqueue(new AuthorizationEventPort.AuthorizationEvent(
                tenantId,
                "RESOURCE_MANIFEST",
                manifestId,
                "RESOURCE_MANIFEST_ACTIVATED",
                Map.of(
                        "applicationId", applicationId,
                        "manifestVersion", Long.toString(manifest.getManifestVersion()),
                        "reason", reason),
                idempotencyKey));
        return new ActivationMutation(tenant.getPolicyVersion(), propagationId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResourceFacade.ApplicationView> applications(String tenantId) {
        return entityManager.createQuery("""
                        select a from ApplicationEntity a
                         where a.tenantId = :tenantId
                         order by a.displayPriority, a.applicationCode
                        """, ApplicationEntity.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .getResultList()
                .stream()
                .map(application -> new ApplicationResourceFacade.ApplicationView(
                        application.getId().toString(),
                        application.getApplicationCode(),
                        application.getApplicationName(),
                        application.getStatus().name(),
                        application.getVersion()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResourceFacade.ResourceView> resources(
            String tenantId,
            String applicationId) {
        return entityManager.createQuery("""
                        select r from ResourceEntity r
                         where r.tenantId = :tenantId and r.applicationId = :applicationId
                         order by r.resourceType, r.resourceCode
                        """, ResourceEntity.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("applicationId", Long.valueOf(applicationId))
                .getResultList()
                .stream()
                .map(this::toResourceView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResourceFacade.ManifestView manifest(
            String tenantId,
            String manifestId) {
        ResourceManifestEntity manifest = requireManifest(tenantId, manifestId);
        return new ApplicationResourceFacade.ManifestView(
                manifest.getId().toString(),
                manifest.getApplicationId().toString(),
                manifest.getStatus().name(),
                manifest.getChecksum(),
                manifest.getManifestVersion());
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResourceFacade.ManifestValidationView validation(
            String tenantId,
            String manifestId) {
        ResourceManifestEntity manifest = requireManifest(tenantId, manifestId);
        Map<String, Object> validation = manifest.getValidationResult();
        return new ApplicationResourceFacade.ManifestValidationView(
                manifestId,
                !Boolean.FALSE.equals(validation.get("valid")),
                strings(validation.get("errors")),
                strings(validation.get("warnings")));
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResourceFacade.ManifestImpactView impact(
            String tenantId,
            String manifestId) {
        ResourceManifestEntity manifest = requireManifest(tenantId, manifestId);
        long pending = entityManager.createQuery("""
                        select count(r) from ResourceEntity r
                         where r.tenantId = :tenantId
                           and r.applicationId = :applicationId
                           and r.sourceManifestId = :manifestId
                        """, Long.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("applicationId", manifest.getApplicationId())
                .setParameter("manifestId", manifest.getId())
                .getSingleResult();
        long stale = entityManager.createQuery("""
                        select count(r) from ResourceEntity r
                         where r.tenantId = :tenantId
                           and r.applicationId = :applicationId
                           and r.status = :status
                        """, Long.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("applicationId", manifest.getApplicationId())
                .setParameter("status", ResourceEntity.Status.STALE)
                .getSingleResult();
        return new ApplicationResourceFacade.ManifestImpactView(
                manifestId, pending, 0L, stale, 0L, List.of());
    }

    @Override
    @Transactional
    public ApplicationResourceFacade.ArchiveResult archive(
            String tenantId,
            String resourceId,
            long expectedVersion,
            String actorId,
            Instant ignoredNow) {
        Instant now = databaseClock.transactionNow();
        ResourceEntity resource = entityManager.find(
                ResourceEntity.class, Long.valueOf(resourceId), LockModeType.PESSIMISTIC_WRITE);
        TenantEntity tenant = entityManager.find(
                TenantEntity.class, Long.valueOf(tenantId), LockModeType.PESSIMISTIC_WRITE);
        if (resource == null || tenant == null
                || !resource.getTenantId().equals(Long.valueOf(tenantId))) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        if (resource.getVersion() != expectedVersion) {
            throw new Rbac3RuleViolation("RESOURCE_VERSION_CONFLICT");
        }
        resource.archive(actorId, now);
        tenant.incrementPolicyVersion(actorId, now);
        eventPort.enqueue(new AuthorizationEventPort.AuthorizationEvent(
                tenantId,
                "RESOURCE",
                resourceId,
                "RESOURCE_ARCHIVED",
                Map.of("policyVersion", Long.toString(tenant.getPolicyVersion())),
                "resource-archive-" + resourceId));
        return new ApplicationResourceFacade.ArchiveResult(
                resourceId, resource.getStatus().name(), tenant.getPolicyVersion());
    }

    private void materializeResources(ManifestFacade.StoredManifest stored, Instant now) {
        Map<String, Long> resourceIds = new LinkedHashMap<>();
        List<TypedResource> resources = resources(stored.manifest());
        resources.forEach(resource -> resourceIds.put(
                resource.type() + ":" + resource.value().code(), idGenerator.nextLongId()));
        for (TypedResource typed : resources) {
            ManifestResource source = typed.value();
            Long parentId = findParentId(resourceIds, source.parentCode());
            Long permissionId = requiredPermissionId(
                    stored.tenantId(), stored.applicationId(), source);
            Map<String, Object> mechanical = new LinkedHashMap<>();
            putIfPresent(mechanical, "path", source.path());
            putIfPresent(mechanical, "routeCode", source.routeCode());
            putIfPresent(mechanical, "gatewayOperationId", source.gatewayOperationId());
            putIfPresent(mechanical, "httpMethod", source.httpMethod());
            putIfPresent(mechanical, "pathPattern", source.pathPattern());
            Long resourceId = resourceIds.get(typed.type() + ":" + source.code());
            ResourceEntity entity = new ResourceEntity(
                    resourceId,
                    Long.valueOf(stored.tenantId()),
                    Long.valueOf(stored.applicationId()),
                    typed.type(),
                    source.code(),
                    source.name() == null ? source.code() : source.name(),
                    parentId,
                    permissionId,
                    Long.valueOf(stored.manifestId()),
                    stored.buildId(),
                    mechanical,
                    Map.copyOf(source.metadata()),
                    "gateway-report",
                    now);
            entityManager.persist(entity);
            if (permissionId != null) {
                entityManager.persist(new PermissionResourceEntity(
                        idGenerator.nextLongId(),
                        Long.valueOf(stored.tenantId()),
                        Long.valueOf(stored.applicationId()),
                        permissionId,
                        resourceId,
                        typed.type(),
                        typed.type() == ResourceEntity.ResourceType.API
                                ? stored.definitionSetId() : null,
                        typed.type() == ResourceEntity.ResourceType.API
                                ? source.gatewayOperationId() : null,
                        source.metadata().get("securityPolicyId"),
                        stored.manifestVersion(),
                        "gateway-report",
                        now));
            }
        }
        materializeFieldDefinitions(stored, resourceIds, now);
    }

    private void materializeFieldDefinitions(
            ManifestFacade.StoredManifest stored,
            Map<String, Long> resourceIds,
            Instant now) {
        for (ResourceManifest.FieldDefinition field : stored.manifest().fieldDefinitions()) {
            Long resourceId = findParentId(resourceIds, field.resourceCode());
            entityManager.persist(new FieldDefinitionEntity(
                    idGenerator.nextLongId(),
                    Long.valueOf(stored.tenantId()),
                    Long.valueOf(stored.applicationId()),
                    resourceId,
                    field.fieldCode(),
                    field.jsonPath(),
                    FieldDefinitionEntity.DataType.valueOf(field.dataType()),
                    FieldDefinitionEntity.Sensitivity.valueOf(field.sensitivity()),
                    FieldDefinitionEntity.DefaultAccess.valueOf(field.defaultAccess()),
                    field.maskingStrategy(),
                    field.writable(),
                    field.exportable(),
                    Long.valueOf(stored.manifestId()),
                    "gateway-report",
                    now));
        }
    }

    private void activateResourceProjection(
            Long tenantId,
            Long applicationId,
            Long manifestId,
            String actorId,
            Instant now) {
        List<ResourceEntity> resources = entityManager.createQuery("""
                        select r from ResourceEntity r
                         where r.tenantId = :tenantId and r.applicationId = :applicationId
                        """, ResourceEntity.class)
                .setParameter("tenantId", tenantId)
                .setParameter("applicationId", applicationId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        for (ResourceEntity resource : resources) {
            if (manifestId.equals(resource.getSourceManifestId())) {
                resource.activate(actorId, now);
            } else {
                resource.markStale(actorId, now);
            }
        }
    }

    private ManifestFacade.StoredManifest toStoredManifest(ResourceManifestEntity entity) {
        ResourceManifest manifest = objectMapper.convertValue(
                entity.getPayload(), ResourceManifest.class);
        return new ManifestFacade.StoredManifest(
                entity.getTenantId().toString(),
                entity.getApplicationId().toString(),
                entity.getId().toString(),
                entity.getDefinitionSetId(),
                entity.getArtifactVersion(),
                entity.getBuildId(),
                entity.getManifestVersion(),
                entity.getChecksum(),
                manifest);
    }

    private ResourceManifestEntity requireManifest(String tenantId, String manifestId) {
        ResourceManifestEntity manifest = entityManager.find(
                ResourceManifestEntity.class, Long.valueOf(manifestId));
        if (manifest == null || !manifest.getTenantId().equals(Long.valueOf(tenantId))) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        return manifest;
    }

    private ApplicationResourceFacade.ResourceView toResourceView(ResourceEntity resource) {
        return new ApplicationResourceFacade.ResourceView(
                resource.getId().toString(),
                resource.getApplicationId().toString(),
                resource.getResourceType().name(),
                resource.getResourceCode(),
                resource.getResourceName(),
                value(resource.getParentResourceId()),
                value(resource.getRequiredPermissionId()),
                resource.getStatus().name(),
                resource.getVersion());
    }

    private Long requiredPermissionId(
            String tenantId,
            String applicationId,
            ManifestResource resource) {
        if (resource.requiredPermissionCode() == null) {
            return null;
        }
        return entityManager.createQuery("""
                        select p from PermissionEntity p
                         where p.tenantId = :tenantId
                           and p.applicationId = :applicationId
                           and p.permissionCode = :permissionCode
                           and p.status <> :archived
                        """, PermissionEntity.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("applicationId", Long.valueOf(applicationId))
                .setParameter("permissionCode", resource.requiredPermissionCode())
                .setParameter("archived", PermissionEntity.Status.ARCHIVED)
                .getResultStream()
                .map(PermissionEntity::getId)
                .findFirst()
                .orElseThrow(() -> new Rbac3RuleViolation("RESOURCE_MANIFEST_INVALID"));
    }

    private static List<String> strings(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        return values.stream().map(String::valueOf).toList();
    }

    private static String value(Long value) {
        return value == null ? null : value.toString();
    }

    private static List<TypedResource> resources(ResourceManifest manifest) {
        List<TypedResource> result = new ArrayList<>();
        add(result, ResourceEntity.ResourceType.APP, manifest.apps());
        add(result, ResourceEntity.ResourceType.MENU, manifest.menus());
        add(result, ResourceEntity.ResourceType.ROUTE, manifest.routes());
        add(result, ResourceEntity.ResourceType.ACTION, manifest.actions());
        add(result, ResourceEntity.ResourceType.API, manifest.apis());
        return result;
    }

    private static void add(
            List<TypedResource> target,
            ResourceEntity.ResourceType type,
            List<ManifestResource> values) {
        values.forEach(value -> target.add(new TypedResource(type, value)));
    }

    private static Long findParentId(Map<String, Long> ids, String parentCode) {
        if (parentCode == null) {
            return null;
        }
        return ids.entrySet().stream()
                .filter(entry -> entry.getKey().endsWith(":" + parentCode))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new Rbac3RuleViolation("RESOURCE_MANIFEST_INVALID"));
    }

    private static void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private record TypedResource(ResourceEntity.ResourceType type, ManifestResource value) {
    }
}
