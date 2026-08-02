package top.egon.cola.platform.rbac3.admin.activation;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.activation.application.RoleActivationFacade;
import top.egon.cola.platform.rbac3.admin.activation.infrastructure.SessionActiveRoleRepository;
import top.egon.cola.platform.rbac3.admin.application.port.AuditPort;
import top.egon.cola.platform.rbac3.admin.application.port.AuthorizationEventPort;
import top.egon.cola.platform.rbac3.admin.session.domain.SessionEntity;
import top.egon.cola.platform.rbac3.admin.session.infrastructure.SessionRepository;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SessionActiveRoleRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-08-01T15:00:00Z");

    @Test
    void staleReplacementUsesThePublicRoleActivationConflictCode() {
        SessionRepository sessions = mock(SessionRepository.class);
        when(sessions.lockByTenantIdAndSessionId(2L, 4L))
                .thenReturn(Optional.of(session()));
        SessionActiveRoleRepository repository = new SessionActiveRoleRepository(
                sessions,
                mock(jakarta.persistence.EntityManager.class),
                mock(LongIdGenerator.class),
                mock(AuditPort.class),
                mock(AuthorizationEventPort.class));
        var command = new RoleActivationFacade.ReplaceCommand(
                "2", "3", "3", "4", List.of("5"), 1L, "3", "command-1");

        assertThatThrownBy(() -> repository.replace(command, NOW, state -> null))
                .isInstanceOf(Rbac3RuleViolation.class)
                .hasMessageContaining("ROLE_ACTIVATION_VERSION_CONFLICT");
    }

    private SessionEntity session() {
        return new SessionEntity(
                1L, 2L, 3L, 4L, 0L, 0L, "family", "device",
                SessionEntity.AuthenticationStrength.PASSWORD,
                NOW.minusSeconds(60), NOW.plusSeconds(1_800),
                NOW.plusSeconds(3_600), "3");
    }
}
