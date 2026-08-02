package top.egon.cola.platform.rbac3.admin.session.application;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthorizationContextFacadeTest {

    private static final Instant NOW = Instant.parse("2026-08-02T03:00:00Z");

    @Test
    void opensOneIdpBoundContextAndRejectsSubjectMismatch() {
        InMemoryStore store = new InMemoryStore();
        AuthorizationContextFacade facade = new AuthorizationContextFacade(
                (tenantId, identitySub) -> Optional.of(
                        new AuthorizationContextFacade.ActiveMembership(
                                tenantId, identitySub, "101", 7, 11)),
                store, () -> 9001L);

        AuthorizationContextFacade.AuthorizationContext opened = facade.open(
                "1", "5001", "alice-sub", NOW, NOW.plusSeconds(3600));
        AuthorizationContextFacade.AuthorizationContext repeated = facade.open(
                "1", "5001", "alice-sub", NOW.plusSeconds(1), NOW.plusSeconds(3600));

        assertThat(repeated).isEqualTo(opened);
        assertThat(opened)
                .extracting(
                        AuthorizationContextFacade.AuthorizationContext::identitySub,
                        AuthorizationContextFacade.AuthorizationContext::rbac3UserId,
                        AuthorizationContextFacade.AuthorizationContext::authVersion,
                        AuthorizationContextFacade.AuthorizationContext::contextVersion,
                        AuthorizationContextFacade.AuthorizationContext::policyVersion)
                .containsExactly("alice-sub", "101", 7L, 0L, 11L);
        assertThatThrownBy(() -> facade.open(
                "1", "5001", "mallory-sub", NOW, NOW.plusSeconds(3600)))
                .isInstanceOf(AuthorizationContextFacade.AuthorizationContextMismatchException.class);
    }

    @Test
    void returnsWinningContextWhenConcurrentCreateAlreadyCommitted() {
        AuthorizationContextFacade.AuthorizationContext winner = context(
                "7001", "1", "5001", "alice-sub", "101");
        AuthorizationContextFacade.AuthorizationContextStore store =
                new AuthorizationContextFacade.AuthorizationContextStore() {
                    private boolean created;

                    @Override
                    public Optional<AuthorizationContextFacade.AuthorizationContext> find(
                            String tenantId, String sessionId) {
                        return created ? Optional.of(winner) : Optional.empty();
                    }

                    @Override
                    public AuthorizationContextFacade.AuthorizationContext create(
                            long entityId,
                            AuthorizationContextFacade.ActiveMembership membership,
                            String sessionId,
                            Instant now,
                            Instant expiresAt) {
                        created = true;
                        throw new AuthorizationContextFacade.ConcurrentContextCreationException();
                    }
                };
        AuthorizationContextFacade facade = new AuthorizationContextFacade(
                (tenantId, identitySub) -> Optional.of(
                        new AuthorizationContextFacade.ActiveMembership(
                                tenantId, identitySub, "101", 7, 11)),
                store, () -> 9001L);

        assertThat(facade.open(
                "1", "5001", "alice-sub", NOW, NOW.plusSeconds(3600)))
                .isEqualTo(winner);
    }

    private static AuthorizationContextFacade.AuthorizationContext context(
            String entityId,
            String tenantId,
            String sessionId,
            String identitySub,
            String rbac3UserId) {
        return new AuthorizationContextFacade.AuthorizationContext(
                entityId, tenantId, sessionId, identitySub, rbac3UserId,
                7, 0, 11, true, "ACTIVE", NOW, NOW.plusSeconds(3600));
    }

    private static final class InMemoryStore
            implements AuthorizationContextFacade.AuthorizationContextStore {

        private final Map<String, AuthorizationContextFacade.AuthorizationContext> values =
                new HashMap<>();

        @Override
        public Optional<AuthorizationContextFacade.AuthorizationContext> find(
                String tenantId, String sessionId) {
            return Optional.ofNullable(values.get(tenantId + ':' + sessionId));
        }

        @Override
        public AuthorizationContextFacade.AuthorizationContext create(
                long entityId,
                AuthorizationContextFacade.ActiveMembership membership,
                String sessionId,
                Instant now,
                Instant expiresAt) {
            var context = new AuthorizationContextFacade.AuthorizationContext(
                    Long.toString(entityId), membership.tenantId(), sessionId,
                    membership.identitySub(), membership.rbac3UserId(),
                    membership.authVersion(), 0, membership.policyVersion(),
                    true, "ACTIVE", now, expiresAt);
            values.put(membership.tenantId() + ':' + sessionId, context);
            return context;
        }
    }
}
