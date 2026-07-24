package top.egon.cola.organization.facade;

import top.egon.cola.organization.facade.user.dto.CreateUserDTO;
import top.egon.cola.organization.facade.user.dto.AssignRoleDTO;
import top.egon.cola.organization.facade.user.dto.GrantPermissionDTO;
import top.egon.cola.organization.facade.teaching.dto.CreateGradeDTO;
import top.egon.cola.organization.facade.teaching.dto.CreateSchoolClassDTO;
import top.egon.cola.organization.facade.teaching.dto.AssignUserToClassDTO;
import top.egon.cola.organization.facade.teaching.SchoolClassFacade;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OrganizationFacadeContractTest {

    private final Validator validator = Validation.byDefaultProvider()
        .configure()
        .messageInterpolator(new ParameterMessageInterpolator())
        .buildValidatorFactory()
        .getValidator();

    @Test
    void validatesCreateUserContractWithoutNormalizingWireData() {
        CreateUserDTO request = new CreateUserDTO("Mario", "MARIO@EXAMPLE.COM");

        assertEquals("MARIO@EXAMPLE.COM", request.email());
        assertFalse(validator.validate(new CreateUserDTO("", "bad")).isEmpty());
    }

    @Test
    void validatesRoleAndPermissionContracts() {
        assertFalse(validator.validate(new AssignRoleDTO("", "STUDENT")).isEmpty());
        assertFalse(validator.validate(new GrantPermissionDTO("STUDENT", "")).isEmpty());
    }

    @Test
    void validatesTeachingContracts() {
        assertFalse(validator.validate(new CreateGradeDTO("", "Grade One")).isEmpty());
        assertFalse(validator.validate(new CreateSchoolClassDTO("Class A", "")).isEmpty());
    }

    @Test
    void requiresGradeIdForSchoolClassRoutingContracts() {
        var getSchoolClass = Arrays.stream(SchoolClassFacade.class.getMethods())
                .filter(method -> method.getName().equals("getSchoolClass"))
                .findFirst()
                .orElseThrow();

        assertEquals(
                List.of(String.class, String.class),
                List.of(getSchoolClass.getParameterTypes()));
        assertEquals(
                List.of("gradeId", "userId", "schoolClassId"),
                Arrays.stream(AssignUserToClassDTO.class.getRecordComponents())
                        .map(component -> component.getName())
                        .toList());
    }
}
