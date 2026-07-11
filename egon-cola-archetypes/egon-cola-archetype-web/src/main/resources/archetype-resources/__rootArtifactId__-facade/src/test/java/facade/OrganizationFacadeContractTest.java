package ${package}.facade;

import ${package}.facade.dto.user.CreateUserDTO;
import ${package}.facade.dto.user.AssignRoleDTO;
import ${package}.facade.dto.user.GrantPermissionDTO;
import ${package}.facade.dto.teaching.CreateGradeDTO;
import ${package}.facade.dto.teaching.CreateSchoolClassDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

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
}
