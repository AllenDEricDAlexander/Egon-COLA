package top.egon.cola.platform.rbac3.admin.session.application;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Opens the tenant authorization context identified by an IdP session without
 * creating any RBAC3 token or refresh family.
 */
public final class AuthorizationContextFacade {

    private final MembershipResolver memberships;
    private final AuthorizationContextStore store;
    private final ContextIdGenerator idGenerator;

    public AuthorizationContextFacade(
            MembershipResolver memberships,
            AuthorizationContextStore store,
            ContextIdGenerator idGenerator) {
        this.memberships = Objects.requireNonNull(memberships, "memberships");
        this.store = Objects.requireNonNull(store, "store");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public AuthorizationContext open(
            String tenantId,
            String sessionId,
            String identitySub,
            Instant now,
            Instant expiresAt) {
        String normalizedTenant = required(tenantId, "tenantId");
        String normalizedSession = required(sessionId, "sessionId");
        String normalizedSub = required(identitySub, "identitySub");
        Objects.requireNonNull(now, "now");
        if (expiresAt == null || !expiresAt.isAfter(now)) {
            throw new IllegalArgumentException("expiresAt must be after now");
        }
        Optional<AuthorizationContext> existing = store.find(
                normalizedTenant, normalizedSession);
        if (existing.isPresent()) {
            AuthorizationContext context = existing.orElseThrow();
            if (!context.identitySub().equals(normalizedSub)
                    || !"ACTIVE".equals(context.status())
                    || !context.expiresAt().isAfter(now)) {
                throw new AuthorizationContextMismatchException(
                        normalizedTenant, normalizedSession, normalizedSub);
            }
            return context;
        }
        ActiveMembership membership = memberships.resolve(
                        normalizedTenant, normalizedSub)
                .orElseThrow(() -> new InactiveIdentityMembershipException(
                        normalizedTenant, normalizedSub));
        if (!membership.tenantId().equals(normalizedTenant)
                || !membership.identitySub().equals(normalizedSub)) {
            throw new IllegalStateException("membership resolver crossed identity boundary");
        }
        return store.create(
                idGenerator.nextId(), membership, normalizedSession, now, expiresAt);
    }

    public AuthorizationContext require(
            String tenantId, String sessionId, String identitySub, Instant now) {
        AuthorizationContext context = store.find(
                        required(tenantId, "tenantId"), required(sessionId, "sessionId"))
                .orElseThrow(() -> new AuthorizationContextMismatchException(
                        tenantId, sessionId, identitySub));
        if (!context.identitySub().equals(required(identitySub, "identitySub"))
                || !"ACTIVE".equals(context.status())
                || !context.expiresAt().isAfter(Objects.requireNonNull(now, "now"))) {
            throw new AuthorizationContextMismatchException(
                    tenantId, sessionId, identitySub);
        }
        return context;
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    @FunctionalInterface
    public interface MembershipResolver {

        Optional<ActiveMembership> resolve(String tenantId, String identitySub);
    }

    public interface AuthorizationContextStore {

        Optional<AuthorizationContext> find(String tenantId, String sessionId);

        AuthorizationContext create(
                long entityId,
                ActiveMembership membership,
                String sessionId,
                Instant now,
                Instant expiresAt);
    }

    @FunctionalInterface
    public interface ContextIdGenerator {

        long nextId();
    }

    @FunctionalInterface
    public interface ContextOpener {

        AuthorizationContext open(
                String tenantId,
                String sessionId,
                String identitySub,
                Instant now,
                Instant expiresAt);
    }

    public record ActiveMembership(
            String tenantId,
            String identitySub,
            String rbac3UserId,
            long authVersion,
            long policyVersion
    ) {
    }

    public record AuthorizationContext(
            String entityId,
            String tenantId,
            String sessionId,
            String identitySub,
            String rbac3UserId,
            long authVersion,
            long contextVersion,
            long policyVersion,
            boolean activationRequired,
            String status,
            Instant createdAt,
            Instant expiresAt
    ) {
    }

    public static final class AuthorizationContextMismatchException
            extends IllegalStateException {

        public AuthorizationContextMismatchException(
                String tenantId, String sessionId, String identitySub) {
            super("authorization context does not match IdP identity: tenantId="
                    + tenantId + ", sessionId=" + sessionId
                    + ", identitySub=" + identitySub);
        }
    }

    public static final class InactiveIdentityMembershipException
            extends IllegalStateException {

        public InactiveIdentityMembershipException(String tenantId, String identitySub) {
            super("active identity membership is required: tenantId="
                    + tenantId + ", identitySub=" + identitySub);
        }
    }
}
