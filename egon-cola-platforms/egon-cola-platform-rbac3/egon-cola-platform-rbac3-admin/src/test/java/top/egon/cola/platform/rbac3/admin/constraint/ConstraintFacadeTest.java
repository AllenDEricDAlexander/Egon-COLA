package top.egon.cola.platform.rbac3.admin.constraint;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.constraint.service.ConstraintFacade;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import top.egon.cola.platform.rbac3.admin.constraint.domain.vo.RoleFactVO;
import top.egon.cola.platform.rbac3.admin.constraint.domain.dto.SodCommandDTO;
import top.egon.cola.platform.rbac3.admin.constraint.domain.enums.ConstraintTypeEnum;

class ConstraintFacadeTest {

    private final ConstraintFacade facade = new ConstraintFacade(roleId -> switch (roleId) {
        case "root-a" -> new RoleFactVO("root-a", "app-a", true);
        case "root-a2" -> new RoleFactVO("root-a2", "app-a", true);
        case "child-a" -> new RoleFactVO("child-a", "app-a", false);
        case "root-b" -> new RoleFactVO("root-b", "app-b", true);
        default -> throw new IllegalArgumentException("unknown role");
    });

    @Test
    void dsdAcceptsOnlyActivationRootsFromTheDeclaredApplication() {
        assertDoesNotThrow(() -> facade.validate(new SodCommandDTO(
                ConstraintTypeEnum.DSD,
                "app-a", List.of("root-a", "root-a2"), 1)));
        Rbac3RuleViolation child = assertThrows(Rbac3RuleViolation.class,
                () -> facade.validate(new SodCommandDTO(
                        ConstraintTypeEnum.DSD,
                        "app-a", List.of("child-a", "root-a2"), 1)));
        assertEquals("DSD_MEMBER_NOT_ACTIVATION_ROOT", child.reasonCode());
        Rbac3RuleViolation crossApplication = assertThrows(Rbac3RuleViolation.class,
                () -> facade.validate(new SodCommandDTO(
                        ConstraintTypeEnum.DSD,
                        "app-a", List.of("root-a", "root-b"), 1)));
        assertEquals("ROLE_APPLICATION_MISMATCH", crossApplication.reasonCode());
    }

    @Test
    void ssdMayQualifyRolesAcrossApplicationsWhenApplicationIsNull() {
        assertDoesNotThrow(() -> facade.validate(new SodCommandDTO(
                ConstraintTypeEnum.SSD,
                null, List.of("root-a", "root-b"), 1)));
    }
}
