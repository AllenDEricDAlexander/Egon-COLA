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
            return validate(existing.orElseThrow(), normalizedTenant,
                    normalizedSession, normalizedSub, now);
        }
        ActiveMembership membership = memberships.resolve(
                        normalizedTenant, normalizedSub)
                .orElseThrow(() -> new InactiveIdentityMembershipException(
                        normalizedTenant, normalizedSub));
        if (!membership.tenantId().equals(normalizedTenant)
                || !membership.identitySub().equals(normalizedSub)) {
            throw new IllegalStateException("membership resolver crossed identity boundary");
        }
        try {
            return store.create(
                    idGenerator.nextId(), membership, normalizedSession, now, expiresAt);
        } catch (ConcurrentContextCreationException exception) {
            AuthorizationContext context = store.find(
                            normalizedTenant, normalizedSession)
                    .orElseThrow(() -> exception);
            return validate(context, normalizedTenant,
                    normalizedSession, normalizedSub, now);
        }
    }

    public AuthorizationContext require(
            String tenantId, String sessionId, String identitySub, Instant now) {
        String normalizedTenant = required(tenantId, "tenantId");
        String normalizedSession = required(sessionId, "sessionId");
        String normalizedSub = required(identitySub, "identitySub");
        AuthorizationContext context = store.find(normalizedTenant, normalizedSession)
                .orElseThrow(() -> new AuthorizationContextMismatchException(
                        normalizedTenant, normalizedSession, normalizedSub));
        return validate(context, normalizedTenant,
                normalizedSession, normalizedSub, Objects.requireNonNull(now, "now"));
    }

    private static AuthorizationContext validate(
            AuthorizationContext context,
            String tenantId,
            String sessionId,
            String identitySub,
            Instant now) {
        if (!context.identitySub().equals(identitySub)
                || !"ACTIVE".equals(context.status())
                || !context.expiresAt().isAfter(now)) {
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
                Instant expiresAt) throws ConcurrentContextCreationException;
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

    public static final class ConcurrentContextCreationException
            extends IllegalStateException {

        public ConcurrentContextCreationException() {
        }

        public ConcurrentContextCreationException(Throwable cause) {
            super(cause);
        }
    }
}
