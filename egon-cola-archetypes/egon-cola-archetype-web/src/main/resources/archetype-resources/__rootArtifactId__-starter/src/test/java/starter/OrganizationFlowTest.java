#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.starter;

import ${package}.adapter.handler.GlobalExceptionHandler;
import ${package}.application.manage.teaching.SchoolClassManage;
import ${package}.application.manage.user.UserManage;
import ${package}.common.constants.ErrorCodes;
import ${package}.common.exceptions.BizException;
import ${package}.common.response.Response;
import ${package}.domain.entities.teaching.SchoolClass;
import ${package}.domain.entities.user.User;
import ${package}.facade.dto.teaching.AssignUserToClassRequest;
import ${package}.facade.teaching.SchoolClassFacade;
import ${package}.facade.user.UserFacade;
import ${package}.infrastructure.repo.teaching.jpa.SchoolClassUserJpaRepository;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(classes = OrganizationApplication.class)
class OrganizationFlowTest {
    @Autowired
    private UserManage userManage;

    @Autowired
    private SchoolClassManage schoolClassManage;

    @Autowired
    private SchoolClassFacade schoolClassFacade;

    @Autowired
    private UserFacade userFacade;

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    @Autowired
    private MockMvc mockMvc;

    @MockitoSpyBean
    private SchoolClassUserJpaRepository schoolClassUserJpaRepository;

    @Test
    void create_user_and_assign_to_school_class() {
        User user = userManage.create("Mario", "mario@example.com");
        SchoolClass schoolClass = schoolClassManage.create("Class One", "Grade One");

        schoolClassManage.assignUser(user.getId(), schoolClass.getId());

        User saved = userManage.getById(user.getId());
        assertThat(saved.getEmail()).isEqualTo("mario@example.com");
        assertThat(saved.getSchoolClassIds()).containsExactly(schoolClass.getId());
    }

    @Test
    void duplicate_assignment_returns_duplicate_error() {
        User user = userManage.create("Luigi", "luigi@example.com");
        SchoolClass schoolClass = schoolClassManage.create("Class Two", "Grade One");
        doThrow(new DataIntegrityViolationException("uk_school_class_user"))
                .when(schoolClassUserJpaRepository)
                .saveAndFlush(any());

        Throwable thrown = catchThrowable(() ->
                schoolClassFacade.assignUser(new AssignUserToClassRequest(user.getId(), schoolClass.getId())));

        assertThat(thrown).isInstanceOf(BizException.class);
        Response response = globalExceptionHandler.handleBizException((BizException) thrown);
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo(ErrorCodes.SCHOOL_CLASS_USER_DUPLICATED);
        assertThat(response.getMessage()).isEqualTo("user already assigned to school class");
    }

    @Test
    void invalid_facade_request_is_rejected_by_validation() {
        Throwable thrown = catchThrowable(() ->
                schoolClassFacade.assignUser(new AssignUserToClassRequest("", "class-id")));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void null_facade_requests_are_rejected_by_validation() {
        assertThat(catchThrowable(() -> userFacade.createUser(null)))
                .isInstanceOf(ConstraintViolationException.class)
                .isNotInstanceOf(NullPointerException.class);
        assertThat(catchThrowable(() -> schoolClassFacade.createSchoolClass(null)))
                .isInstanceOf(ConstraintViolationException.class)
                .isNotInstanceOf(NullPointerException.class);
        assertThat(catchThrowable(() -> schoolClassFacade.assignUser(null)))
                .isInstanceOf(ConstraintViolationException.class)
                .isNotInstanceOf(NullPointerException.class);
    }

    @Test
    void invalid_controller_request_returns_validation_error() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_ERROR))
                .andExpect(jsonPath("$.message").value("validation error"));
    }
}
