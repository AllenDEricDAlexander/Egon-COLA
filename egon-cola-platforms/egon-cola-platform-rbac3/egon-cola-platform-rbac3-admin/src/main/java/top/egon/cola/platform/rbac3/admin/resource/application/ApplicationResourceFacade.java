package top.egon.cola.platform.rbac3.admin.resource.application;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Exposes tenant-scoped application, resource, and immutable manifest queries.
 */
public final class ApplicationResourceFacade {

    private final Store store;

    public ApplicationResourceFacade(Store store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    public List<ApplicationView> applications(String tenantId) {
        return List.copyOf(store.applications(required(tenantId, "tenantId")));
    }

    public List<ResourceView> resources(String tenantId, String applicationId) {
        return List.copyOf(store.resources(
                required(tenantId, "tenantId"),
                required(applicationId, "applicationId")));
    }

    public ManifestView manifest(String tenantId, String manifestId) {
        return store.manifest(
                required(tenantId, "tenantId"),
                required(manifestId, "manifestId"));
    }

    public ManifestValidationView validation(String tenantId, String manifestId) {
        return store.validation(
                required(tenantId, "tenantId"),
                required(manifestId, "manifestId"));
    }

    public ManifestImpactView impact(String tenantId, String manifestId) {
        return store.impact(
                required(tenantId, "tenantId"),
                required(manifestId, "manifestId"));
    }

    public ArchiveResult archive(
            String tenantId,
            String resourceId,
            long expectedVersion,
            String actorId,
            Instant now) {
        if (expectedVersion < 0L) {
            throw new IllegalArgumentException("expectedVersion must not be negative");
        }
        return store.archive(
                required(tenantId, "tenantId"),
                required(resourceId, "resourceId"),
                expectedVersion,
                required(actorId, "actorId"),
                Objects.requireNonNull(now, "now"));
    }

    public interface Store {

        List<ApplicationView> applications(String tenantId);

        List<ResourceView> resources(String tenantId, String applicationId);

        ManifestView manifest(String tenantId, String manifestId);

        ManifestValidationView validation(String tenantId, String manifestId);

        ManifestImpactView impact(String tenantId, String manifestId);

        ArchiveResult archive(
                String tenantId,
                String resourceId,
                long expectedVersion,
                String actorId,
                Instant now);
    }

    public record ApplicationView(
            String applicationId,
            String applicationCode,
            String applicationName,
            String status,
            long version) {
    }

    public record ResourceView(
            String resourceId,
            String applicationId,
            String resourceType,
            String resourceCode,
            String resourceName,
            String parentResourceId,
            String requiredPermissionId,
            String status,
            long version) {
    }

    public record ManifestView(
            String manifestId,
            String applicationId,
            String status,
            String checksum,
            long manifestVersion) {
    }

    public record ManifestValidationView(
            String manifestId,
            boolean valid,
            List<String> errors,
            List<String> warnings) {

        public ManifestValidationView {
            errors = List.copyOf(errors);
            warnings = List.copyOf(warnings);
        }
    }

    public record ManifestImpactView(
            String manifestId,
            long resourcesAdded,
            long resourcesChanged,
            long resourcesStale,
            long affectedRoleCount,
            List<String> conflicts) {

        public ManifestImpactView {
            conflicts = List.copyOf(conflicts);
        }
    }

    public record ArchiveResult(String resourceId, String status, long policyVersion) {
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
