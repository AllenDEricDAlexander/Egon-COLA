package top.egon.cola.platform.rbac3.admin.resource.application;

import top.egon.cola.platform.rbac3.contract.manifest.ManifestResource;
import top.egon.cola.platform.rbac3.contract.manifest.ResourceManifest;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Validates immutable build manifests and coordinates activation propagation.
 */
public final class ManifestFacade {

    private static final String SUPPORTED_SCHEMA_VERSION = "1";

    private final ManifestStore manifestStore;
    private final ComponentKeyRegistry componentKeyRegistry;

    public ManifestFacade(
            ManifestStore manifestStore,
            ComponentKeyRegistry componentKeyRegistry) {
        this.manifestStore = Objects.requireNonNull(manifestStore, "manifestStore");
        this.componentKeyRegistry = Objects.requireNonNull(
                componentKeyRegistry, "componentKeyRegistry");
    }

    public SubmissionResult submit(SubmitCommand command) {
        Objects.requireNonNull(command, "command");
        validate(command);
        ResourceManifest manifest = command.manifest();
        Optional<StoredManifest> existing = manifestStore.findByBuild(
                command.tenantId(),
                command.applicationId(),
                manifest.artifactVersion(),
                manifest.buildId());
        if (existing.isPresent()) {
            if (existing.get().checksum().equals(manifest.checksum())) {
                return new SubmissionResult(
                        SubmissionOutcome.IDEMPOTENT, existing.get().manifestId());
            }
            throw new Rbac3RuleViolation("RESOURCE_MANIFEST_CONFLICT");
        }
        StoredManifest stored = new StoredManifest(
                command.tenantId(),
                command.applicationId(),
                command.manifestId(),
                command.definitionSetId(),
                manifest.artifactVersion(),
                manifest.buildId(),
                manifest.manifestVersion(),
                manifest.checksum(),
                manifest);
        manifestStore.insert(stored);
        return new SubmissionResult(SubmissionOutcome.ACCEPTED, stored.manifestId());
    }

    public ActivationResult activate(ActivateCommand command, Instant now) {
        Objects.requireNonNull(command, "command");
        ManifestStore.ActivationMutation mutation = manifestStore.activate(
                command.tenantId(),
                command.applicationId(),
                command.manifestId(),
                command.expectedApplicationVersion(),
                command.expectedCurrentManifestVersion(),
                command.expectedDefinitionSetId(),
                command.actorId(),
                command.idempotencyKey(),
                command.reason(),
                now);
        return new ActivationResult(
                command.manifestId(),
                mutation.policyVersion(),
                mutation.propagationId(),
                mutation.propagationPending());
    }

    private void validate(SubmitCommand command) {
        ResourceManifest manifest = command.manifest();
        if (!SUPPORTED_SCHEMA_VERSION.equals(manifest.schemaVersion())) {
            throw new Rbac3RuleViolation("RESOURCE_MANIFEST_INVALID");
        }
        validateComponentKeys(manifest.routes());
        validateComponentKeys(manifest.actions());
        Set<String> resourceCodes = new HashSet<>();
        validateUniqueCodes(resourceCodes, manifest.apps());
        validateUniqueCodes(resourceCodes, manifest.menus());
        validateUniqueCodes(resourceCodes, manifest.routes());
        validateUniqueCodes(resourceCodes, manifest.actions());
        validateUniqueCodes(resourceCodes, manifest.apis());
        Map<String, ResourceKind> kinds = resourceKinds(manifest);
        validateHierarchy(manifest, kinds);
        validateFields(manifest, kinds.keySet());
        Set<String> operationIds = new HashSet<>();
        for (ManifestResource api : manifest.apis()) {
            if (api.gatewayOperationId() == null
                    || api.gatewayOperationId().length() > 64
                    || api.httpMethod() == null
                    || api.pathPattern() == null
                    || !operationIds.add(api.gatewayOperationId())
                    || api.requiredPermissionCode() == null
                    && !Boolean.TRUE.equals(api.externalAccessible())) {
                throw new Rbac3RuleViolation("RESOURCE_MANIFEST_INVALID");
            }
        }
    }

    private void validateComponentKeys(List<ManifestResource> resources) {
        for (ManifestResource resource : resources) {
            if (resource.componentKey() != null
                    && !componentKeyRegistry.known(resource.componentKey())) {
                throw new Rbac3RuleViolation("RESOURCE_MANIFEST_INVALID");
            }
        }
    }

    private void validateUniqueCodes(
            Set<String> resourceCodes,
            List<ManifestResource> resources) {
        for (ManifestResource resource : resources) {
            if (!resource.code().matches("^[a-z][a-z0-9-]{1,127}$")
                    || !resourceCodes.add(resource.code())) {
                throw new Rbac3RuleViolation("RESOURCE_MANIFEST_INVALID");
            }
        }
    }

    private Map<String, ResourceKind> resourceKinds(ResourceManifest manifest) {
        Map<String, ResourceKind> result = new LinkedHashMap<>();
        put(result, ResourceKind.APP, manifest.apps());
        put(result, ResourceKind.MENU, manifest.menus());
        put(result, ResourceKind.ROUTE, manifest.routes());
        put(result, ResourceKind.ACTION, manifest.actions());
        put(result, ResourceKind.API, manifest.apis());
        return result;
    }

    private void validateHierarchy(
            ResourceManifest manifest,
            Map<String, ResourceKind> kinds) {
        validateParents(manifest.apps(), kinds);
        validateParents(manifest.menus(), kinds, ResourceKind.APP, ResourceKind.MENU);
        validateParents(manifest.routes(), kinds, ResourceKind.APP, ResourceKind.MENU);
        validateParents(manifest.actions(), kinds, ResourceKind.ROUTE);
        validateParents(manifest.apis(), kinds);
        for (ManifestResource route : manifest.routes()) {
            if (route.path() == null || route.componentKey() == null) {
                throw new Rbac3RuleViolation("RESOURCE_MANIFEST_INVALID");
            }
        }
    }

    private void validateParents(
            List<ManifestResource> resources,
            Map<String, ResourceKind> kinds,
            ResourceKind... allowedParents) {
        Set<ResourceKind> allowed = Set.of(allowedParents);
        for (ManifestResource resource : resources) {
            if (resource.parentCode() == null) {
                if (!allowed.isEmpty()) {
                    throw new Rbac3RuleViolation("RESOURCE_MANIFEST_INVALID");
                }
                continue;
            }
            ResourceKind parent = kinds.get(resource.parentCode());
            if (parent == null || !allowed.contains(parent)) {
                throw new Rbac3RuleViolation("RESOURCE_MANIFEST_INVALID");
            }
        }
    }

    private void validateFields(ResourceManifest manifest, Set<String> resourceCodes) {
        Set<String> fieldIdentities = new HashSet<>();
        for (ResourceManifest.FieldDefinition field : manifest.fieldDefinitions()) {
            if (!resourceCodes.contains(field.resourceCode())
                    || !fieldIdentities.add(field.resourceCode() + ':' + field.fieldCode())) {
                throw new Rbac3RuleViolation("RESOURCE_MANIFEST_INVALID");
            }
        }
    }

    private static void put(
            Map<String, ResourceKind> target,
            ResourceKind kind,
            List<ManifestResource> resources) {
        resources.forEach(resource -> target.put(resource.code(), kind));
    }

    private enum ResourceKind {
        APP,
        MENU,
        ROUTE,
        ACTION,
        API
    }

    public interface ManifestStore {

        Optional<StoredManifest> findByBuild(
                String tenantId,
                String applicationId,
                String artifactVersion,
                String buildId);

        void insert(StoredManifest manifest);

        default ActivationMutation activate(
                String tenantId,
                String applicationId,
                String manifestId,
                long expectedApplicationVersion,
                long expectedCurrentManifestVersion,
                String expectedDefinitionSetId,
                String actorId,
                String idempotencyKey,
                String reason,
                Instant now) {
            throw new UnsupportedOperationException("manifest activation is not configured");
        }

        record ActivationMutation(
                long policyVersion,
                String propagationId,
                boolean propagationPending
        ) {
        }
    }

    @FunctionalInterface
    public interface ComponentKeyRegistry {

        boolean known(String componentKey);
    }

    public record SubmitCommand(
            String tenantId,
            String applicationId,
            String manifestId,
            String definitionSetId,
            ResourceManifest manifest
    ) {

        public SubmitCommand {
            tenantId = required(tenantId, "tenantId");
            applicationId = required(applicationId, "applicationId");
            manifestId = required(manifestId, "manifestId");
            definitionSetId = required(definitionSetId, "definitionSetId");
            manifest = Objects.requireNonNull(manifest, "manifest");
        }
    }

    public record StoredManifest(
            String tenantId,
            String applicationId,
            String manifestId,
            String definitionSetId,
            String artifactVersion,
            String buildId,
            long manifestVersion,
            String checksum,
            ResourceManifest manifest
    ) {
    }

    public record ActivateCommand(
            String tenantId,
            String applicationId,
            String manifestId,
            long expectedApplicationVersion,
            long expectedCurrentManifestVersion,
            String expectedDefinitionSetId,
            String actorId,
            String idempotencyKey,
            String reason
    ) {

        public ActivateCommand {
            tenantId = required(tenantId, "tenantId");
            applicationId = required(applicationId, "applicationId");
            manifestId = required(manifestId, "manifestId");
            expectedDefinitionSetId = required(
                    expectedDefinitionSetId, "expectedDefinitionSetId");
            actorId = required(actorId, "actorId");
            idempotencyKey = required(idempotencyKey, "idempotencyKey");
            reason = required(reason, "reason");
            if (expectedApplicationVersion < 0L || expectedCurrentManifestVersion < 0L) {
                throw new IllegalArgumentException("manifest versions must not be negative");
            }
        }
    }

    public record SubmissionResult(SubmissionOutcome outcome, String manifestId) {
    }

    public record ActivationResult(
            String manifestId,
            long policyVersion,
            String propagationId,
            boolean propagationPending
    ) {
    }

    public enum SubmissionOutcome {
        ACCEPTED,
        IDEMPOTENT
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
