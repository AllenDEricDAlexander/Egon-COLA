package top.egon.cola.organization.facade;

import top.egon.cola.organization.facade.user.dto.AssignRoleDTO;
import top.egon.cola.organization.facade.user.dto.CreateUserDTO;
import top.egon.cola.organization.facade.user.dto.GrantPermissionDTO;
import top.egon.cola.organization.facade.teaching.dto.AssignUserToClassDTO;
import top.egon.cola.organization.facade.teaching.dto.CreateGradeDTO;
import top.egon.cola.organization.facade.teaching.dto.CreateSchoolClassDTO;
import top.egon.cola.organization.facade.teaching.dto.GradeDetailDTO;
import top.egon.cola.organization.facade.teaching.GradeFacade;
import top.egon.cola.organization.facade.teaching.SchoolClassFacade;
import top.egon.cola.organization.facade.teaching.dto.SchoolClassDetailDTO;
import top.egon.cola.organization.facade.user.PermissionFacade;
import top.egon.cola.organization.facade.user.UserFacade;
import top.egon.cola.organization.facade.user.dto.PermissionTreeDTO;
import top.egon.cola.organization.facade.user.dto.UserDetailDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.Test;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertFalse(validator.validate(new AssignRoleDTO(0L, "STUDENT")).isEmpty());
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
                List.of(Long.class, Long.class),
                List.of(getSchoolClass.getParameterTypes()));
        assertEquals(
                List.of("gradeId", "userId", "schoolClassId"),
                Arrays.stream(AssignUserToClassDTO.class.getRecordComponents())
                        .map(component -> component.getName())
                        .toList());
    }

    @Test
    void exposesPositiveLongIdentityAtEveryOrganizationBoundary() throws Exception {
        assertPositiveLongParameter(UserFacade.class, "getUser", 0);
        assertPositiveLongParameter(PermissionFacade.class, "getPermissionTree", 0);
        assertPositiveLongParameter(GradeFacade.class, "getGrade", 0);
        assertPositiveLongParameter(SchoolClassFacade.class, "getSchoolClass", 0);
        assertPositiveLongParameter(SchoolClassFacade.class, "getSchoolClass", 1);

        assertPositiveLongRecord(UserDetailDTO.class, "id");
        assertPositiveLongRecord(PermissionTreeDTO.class, "userId");
        assertPositiveLongRecord(GradeDetailDTO.class, "id");
        assertPositiveLongRecord(SchoolClassDetailDTO.class, "id");
        var userIds = SchoolClassDetailDTO.class.getRecordComponents()[5];
        assertEquals(List.class, userIds.getType());
        assertEquals(Long.class,
                ((ParameterizedType) userIds.getGenericType()).getActualTypeArguments()[0]);
    }

    @Test
    void preservesLargeLongIdentityWithoutNarrowing() throws Exception {
        long snowflake = 9_007_199_254_740_993L;
        var constructor = Arrays.stream(UserDetailDTO.class.getDeclaredConstructors())
                .filter(candidate -> candidate.getParameterTypes().length == 5
                        && candidate.getParameterTypes()[0].equals(Long.class))
                .findFirst()
                .orElseThrow();
        Object detail = constructor.newInstance(
                snowflake, "Mario", "mario@example.com", "ACTIVE", List.of("ADMIN"));
        assertEquals(snowflake, UserDetailDTO.class.getMethod("id").invoke(detail));
    }

    private static void assertPositiveLongParameter(
            Class<?> facadeType, String methodName, int parameterIndex) throws NoSuchMethodException {
        Method method = Arrays.stream(facadeType.getMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
        assertEquals(Long.class, method.getParameterTypes()[parameterIndex]);
        assertTrue(Arrays.stream(method.getParameterAnnotations()[parameterIndex])
                .anyMatch(annotation -> annotation.annotationType().equals(NotNull.class)));
        assertTrue(Arrays.stream(method.getParameterAnnotations()[parameterIndex])
                .anyMatch(annotation -> annotation.annotationType().equals(Positive.class)));
    }

    private static void assertPositiveLongRecord(Class<?> type, String componentName)
            throws NoSuchMethodException {
        var component = Arrays.stream(type.getRecordComponents())
                .filter(candidate -> candidate.getName().equals(componentName))
                .findFirst()
                .orElseThrow();
        assertEquals(Long.class, component.getType());
        Method accessor = component.getAccessor();
        assertTrue(accessor.isAnnotationPresent(NotNull.class));
        assertTrue(accessor.isAnnotationPresent(Positive.class));
    }
}
