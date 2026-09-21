package top.egon.cola.archetype.source.lightopen.adapter.teaching.facade;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.archetype.source.lightopen.adapter.teaching.facade.impl.CourseFacadeImpl;
import top.egon.cola.archetype.source.lightopen.application.teaching.manage.CourseManage;
import top.egon.cola.archetype.source.lightopen.common.exception.TeachingFacadeException;
import top.egon.cola.archetype.source.lightopen.common.exception.TeachingUseCaseException;
import top.egon.cola.archetype.source.lightopen.facade.teaching.dto.CreateCourseDTO;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TeachingFacadeImplTest {
    private final ValidatorFactory validators = Validation.buildDefaultValidatorFactory();
    private final CourseManage manage = mock(CourseManage.class);
    private final CourseFacadeImpl facade = new CourseFacadeImpl(manage, new ValidationUtils(validators.getValidator()));

    @AfterEach
    void closeValidationFactory() {
        validators.close();
    }

    @Test
    void maps_application_failure_to_facade_failure() {
        when(manage.create(any())).thenThrow(new TeachingUseCaseException(
                "COURSE_EXISTS", "Course exists", new IllegalStateException("internal")));

        assertThatThrownBy(() -> facade.createCourse(new CreateCourseDTO("MATH", "Math", "operator-1", "request-1")))
                .isInstanceOfSatisfying(TeachingFacadeException.class, error -> {
                    assertThat(error.getStatus()).isEqualTo("COURSE_EXISTS");
                    assertThat(error.getCause()).isNull();
                });
    }

    @Test
    void rejects_a_blank_carrier_before_the_use_case_runs() {
        assertThatThrownBy(() -> facade.createCourse(new CreateCourseDTO("MATH", " ", "operator-1", "request-1")))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        org.mockito.Mockito.verifyNoInteractions(manage);
    }
}
