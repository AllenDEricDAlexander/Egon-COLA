package top.egon.cola.platform.rbac3.admin.constraint;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.constraint.application.ConstraintFacade;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConstraintFacadeTest {

    private final ConstraintFacade facade = new ConstraintFacade(roleId -> switch (roleId) {
        case "root-a" -> new ConstraintFacade.RoleFact("root-a", "app-a", true);
        case "root-a2" -> new ConstraintFacade.RoleFact("root-a2", "app-a", true);
        case "child-a" -> new ConstraintFacade.RoleFact("child-a", "app-a", false);
        case "root-b" -> new ConstraintFacade.RoleFact("root-b", "app-b", true);
        default -> throw new IllegalArgumentException("unknown role");
    });

    @Test
    void dsdAcceptsOnlyActivationRootsFromTheDeclaredApplication() {
        assertDoesNotThrow(() -> facade.validate(new ConstraintFacade.SodCommand(
                ConstraintFacade.ConstraintType.DSD,
                "app-a", List.of("root-a", "root-a2"), 1)));
        Rbac3RuleViolation child = assertThrows(Rbac3RuleViolation.class,
                () -> facade.validate(new ConstraintFacade.SodCommand(
                        ConstraintFacade.ConstraintType.DSD,
                        "app-a", List.of("child-a", "root-a2"), 1)));
        assertEquals("DSD_MEMBER_NOT_ACTIVATION_ROOT", child.reasonCode());
        Rbac3RuleViolation crossApplication = assertThrows(Rbac3RuleViolation.class,
                () -> facade.validate(new ConstraintFacade.SodCommand(
                        ConstraintFacade.ConstraintType.DSD,
                        "app-a", List.of("root-a", "root-b"), 1)));
        assertEquals("ROLE_APPLICATION_MISMATCH", crossApplication.reasonCode());
    }

    @Test
    void ssdMayQualifyRolesAcrossApplicationsWhenApplicationIsNull() {
        assertDoesNotThrow(() -> facade.validate(new ConstraintFacade.SodCommand(
                ConstraintFacade.ConstraintType.SSD,
                null, List.of("root-a", "root-b"), 1)));
    }
}
