package top.egon.cola.platform.rbac3.admin.identity.application;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Owns the one-to-one mapping between a global IdP identity and a tenant user.
 */
public final class IdentityMappingFacade {

    private final MappingStore store;
    private final MappingIdGenerator idGenerator;

    public IdentityMappingFacade(MappingStore store, MappingIdGenerator idGenerator) {
        this.store = Objects.requireNonNull(store, "store");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public Mapping bind(
            String tenantId,
            String identitySub,
            String rbac3UserId,
            String actorId,
            Instant now) {
        String normalizedTenantId = required(tenantId, "tenantId");
        String normalizedSub = required(identitySub, "identitySub");
        String normalizedUserId = required(rbac3UserId, "rbac3UserId");
        required(actorId, "actorId");
        Objects.requireNonNull(now, "now");
        Optional<Mapping> existing = store.find(normalizedTenantId, normalizedSub);
        if (existing.isPresent()) {
            Mapping mapping = existing.orElseThrow();
            if (!mapping.rbac3UserId().equals(normalizedUserId)) {
                throw new DuplicateIdentityMappingException(
                        normalizedTenantId, normalizedSub, mapping.rbac3UserId());
            }
            return mapping;
        }
        return store.create(
                idGenerator.nextId(), normalizedTenantId, normalizedSub,
                normalizedUserId, actorId.trim(), now);
    }

    public Optional<ResolvedMembership> resolve(
            String identitySub,
            String tenantId,
            String clientId) {
        required(clientId, "clientId");
        return store.resolve(
                required(tenantId, "tenantId"), required(identitySub, "identitySub"));
    }

    public List<TenantMembership> tenants(String identitySub, String clientId) {
        required(clientId, "clientId");
        return List.copyOf(store.tenants(required(identitySub, "identitySub")));
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    public interface MappingStore {

        Optional<Mapping> find(String tenantId, String identitySub);

        Mapping create(
                long mappingId,
                String tenantId,
                String identitySub,
                String rbac3UserId,
                String actorId,
                Instant now);

        Optional<ResolvedMembership> resolve(String tenantId, String identitySub);

        List<TenantMembership> tenants(String identitySub);
    }

    @FunctionalInterface
    public interface MappingIdGenerator {

        long nextId();
    }

    public record Mapping(
            String mappingId,
            String tenantId,
            String identitySub,
            String rbac3UserId,
            boolean active,
            Instant updatedAt
    ) {
    }

    public record ResolvedMembership(
            String tenantId,
            String tenantCode,
            String tenantName,
            String identitySub,
            String rbac3UserId,
            String displayName,
            boolean authorizationContextRequired,
            long authVersion,
            long policyVersion
    ) {
    }

    public record TenantMembership(
            String tenantId,
            String tenantCode,
            String tenantName,
            String rbac3UserId,
            String displayName
    ) {
    }

    public static final class DuplicateIdentityMappingException
            extends IllegalStateException {

        public DuplicateIdentityMappingException(
                String tenantId, String identitySub, String existingUserId) {
            super("identity already maps to another tenant user: tenantId="
                    + tenantId + ", identitySub=" + identitySub
                    + ", rbac3UserId=" + existingUserId);
        }
    }
}
