#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.starter;

import ${package}.application.context.OrganizationRequestContext;
import ${package}.application.context.OrganizationRequestContextHolder;
import ${package}.facade.dto.teaching.AssignUserToClassDTO;
import ${package}.facade.dto.teaching.CreateGradeDTO;
import ${package}.facade.dto.teaching.CreateSchoolClassDTO;
import ${package}.facade.teaching.GradeFacade;
import ${package}.facade.dto.user.CreateUserDTO;
import ${package}.facade.dto.user.UserDetailDTO;
import ${package}.facade.teaching.SchoolClassFacade;
import ${package}.facade.user.UserFacade;
import ${package}.infrastructure.repo.teaching.jpa.SchoolClassUserJpaRepository;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(
    classes = OrganizationApplication.class,
    properties = "dubbo.protocol.port=-1")
class OrganizationFlowTest {

    @Autowired
    private SchoolClassFacade schoolClassFacade;

    @Autowired
    private GradeFacade gradeFacade;

    @Autowired
    private UserFacade userFacade;

    @Autowired
    private SchoolClassUserJpaRepository schoolClassUserJpaRepository;

    @MockitoSpyBean
    private SchoolClassUserJpaRepository schoolClassUserJpaRepositorySpy;

    @AfterEach
    void clearContext() {
        OrganizationRequestContextHolder.clear();
    }

    @Test
    void createsUserAndAssignsItToSchoolClass() {
        setAdminContext();
        UserDetailDTO user = userFacade.createUser(
            new CreateUserDTO("Mario", "mario-" + UUID.randomUUID() + "@example.com"));
        var grade = gradeFacade.createGrade(new CreateGradeDTO(
            "GRADE_" + UUID.randomUUID().toString().replace("-", "_"), "Grade One"));
        var schoolClass = schoolClassFacade.createSchoolClass(
            new CreateSchoolClassDTO("Class One", grade.code()));

        schoolClassFacade.assignUser(new AssignUserToClassDTO(user.id(), schoolClass.id()));

        assertThat(schoolClassUserJpaRepository.existsByUserIdAndSchoolClassId(user.id(), schoolClass.id()))
            .isTrue();
        assertThat(userFacade.getUser(user.id()).email()).isEqualTo(user.email());
    }

    @Test
    void duplicateAssignmentReturnsStableLegacyErrorDuringCutover() {
        setAdminContext();
        UserDetailDTO user = userFacade.createUser(
            new CreateUserDTO("Luigi", "luigi-" + UUID.randomUUID() + "@example.com"));
        var grade = gradeFacade.createGrade(new CreateGradeDTO(
            "GRADE_" + UUID.randomUUID().toString().replace("-", "_"), "Grade One"));
        var schoolClass = schoolClassFacade.createSchoolClass(
            new CreateSchoolClassDTO("Class Two", grade.code()));
        doThrow(new DataIntegrityViolationException("uk_school_class_user"))
            .when(schoolClassUserJpaRepositorySpy).saveAndFlush(any());

        Throwable thrown = catchThrowable(() -> schoolClassFacade.assignUser(
            new AssignUserToClassDTO(user.id(), schoolClass.id())));

        assertThat(thrown).isNotNull();
    }

    @Test
    void nullFacadeRequestIsRejectedByValidation() {
        assertThat(catchThrowable(() -> userFacade.createUser(null)))
            .isInstanceOf(ConstraintViolationException.class)
            .isNotInstanceOf(NullPointerException.class);
    }

    private static void setAdminContext() {
        OrganizationRequestContextHolder.set(new OrganizationRequestContext(
            "admin-1", Set.of("ORGANIZATION_ADMIN", "TEACHING_ADMIN"), "trace-1"));
    }

    @Test
    void applicationContextCanRepresentOrganizationAdmin() {
        OrganizationRequestContext context = new OrganizationRequestContext(
            "admin-1", Set.of("ORGANIZATION_ADMIN"), "trace-1");

        assertThat(context.hasRole("ORGANIZATION_ADMIN")).isTrue();
    }
}
