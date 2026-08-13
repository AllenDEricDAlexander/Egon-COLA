package top.egon.cola.platform.rbac3.admin.session.application;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import top.egon.cola.platform.rbac3.admin.session.repository.AuthorizationContextRepository;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.ActiveMembershipVO;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.AuthorizationContextVO;
import top.egon.cola.platform.rbac3.admin.session.domain.exception.AuthorizationContextMismatchException;
import top.egon.cola.platform.rbac3.admin.session.domain.exception.ConcurrentContextCreationException;
import top.egon.cola.platform.rbac3.admin.session.service.AuthorizationContextFacade;

class AuthorizationContextFacadeTest {

    private static final Instant NOW = Instant.parse("2026-08-02T03:00:00Z");

    @Test
    void opensOneIdpBoundContextAndRejectsSubjectMismatch() {
        InMemoryStore store = new InMemoryStore();
        AuthorizationContextFacade facade = new AuthorizationContextFacade(
                (tenantId, identitySub) -> Optional.of(
                        new ActiveMembershipVO(
                                tenantId, identitySub, "101", 7, 11)),
                store, () -> 9001L);

        AuthorizationContextVO opened = facade.open(
                "1", "5001", "alice-sub", NOW, NOW.plusSeconds(3600));
        AuthorizationContextVO repeated = facade.open(
                "1", "5001", "alice-sub", NOW.plusSeconds(1), NOW.plusSeconds(3600));

        assertThat(repeated).isEqualTo(opened);
        assertThat(opened)
                .extracting(
                        AuthorizationContextVO::identitySub,
                        AuthorizationContextVO::rbac3UserId,
                        AuthorizationContextVO::authVersion,
                        AuthorizationContextVO::contextVersion,
                        AuthorizationContextVO::policyVersion)
                .containsExactly("alice-sub", "101", 7L, 0L, 11L);
        assertThatThrownBy(() -> facade.open(
                "1", "5001", "mallory-sub", NOW, NOW.plusSeconds(3600)))
                .isInstanceOf(AuthorizationContextMismatchException.class);
    }

    @Test
    void returnsWinningContextWhenConcurrentCreateAlreadyCommitted() {
        AuthorizationContextVO winner = context(
                "7001", "1", "5001", "alice-sub", "101");
        AuthorizationContextRepository store =
                new AuthorizationContextRepository() {
                    private boolean created;

                    @Override
                    public Optional<AuthorizationContextVO> find(
                            String tenantId, String sessionId) {
                        return created ? Optional.of(winner) : Optional.empty();
                    }

                    @Override
                    public AuthorizationContextVO create(
                            long entityId,
                            ActiveMembershipVO membership,
                            String sessionId,
                            Instant now,
                            Instant expiresAt) {
                        created = true;
                        throw new ConcurrentContextCreationException();
                    }
                };
        AuthorizationContextFacade facade = new AuthorizationContextFacade(
                (tenantId, identitySub) -> Optional.of(
                        new ActiveMembershipVO(
                                tenantId, identitySub, "101", 7, 11)),
                store, () -> 9001L);

        assertThat(facade.open(
                "1", "5001", "alice-sub", NOW, NOW.plusSeconds(3600)))
                .isEqualTo(winner);
    }

    private static AuthorizationContextVO context(
            String entityId,
            String tenantId,
            String sessionId,
            String identitySub,
            String rbac3UserId) {
        return new AuthorizationContextVO(
                entityId, tenantId, sessionId, identitySub, rbac3UserId,
                7, 0, 11, true, "ACTIVE", NOW, NOW.plusSeconds(3600));
    }

    private static final class InMemoryStore
            implements AuthorizationContextRepository {

        private final Map<String, AuthorizationContextVO> values =
                new HashMap<>();

        @Override
        public Optional<AuthorizationContextVO> find(
                String tenantId, String sessionId) {
            return Optional.ofNullable(values.get(tenantId + ':' + sessionId));
        }

        @Override
        public AuthorizationContextVO create(
                long entityId,
                ActiveMembershipVO membership,
                String sessionId,
                Instant now,
                Instant expiresAt) {
            var context = new AuthorizationContextVO(
                    Long.toString(entityId), membership.tenantId(), sessionId,
                    membership.identitySub(), membership.rbac3UserId(),
                    membership.authVersion(), 0, membership.policyVersion(),
                    true, "ACTIVE", now, expiresAt);
            values.put(membership.tenantId() + ':' + sessionId, context);
            return context;
        }
    }
}
