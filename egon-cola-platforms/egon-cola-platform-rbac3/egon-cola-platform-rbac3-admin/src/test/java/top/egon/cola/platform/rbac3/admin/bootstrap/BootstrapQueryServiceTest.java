package top.egon.cola.platform.rbac3.admin.bootstrap;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.bootstrap.service.BootstrapQueryService;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BootstrapQueryServiceTest {

    @Test
    void requiresANonemptyActiveRoleSnapshot() {
        BootstrapQueryService service = new BootstrapQueryService(
                (tenantId, userId, sessionId) -> Optional.empty());
        Rbac3RuleViolation error = assertThrows(Rbac3RuleViolation.class,
                () -> service.query("tenant", "user", "session"));
        assertEquals("ROLE_ACTIVATION_REQUIRED", error.reasonCode());
    }
}
