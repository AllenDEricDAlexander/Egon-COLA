package top.egon.cola.archetype.source.light.adapter.teaching.facade;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import top.egon.cola.archetype.source.light.adapter.pojo.convertor.LightFacadeConverter;
import top.egon.cola.archetype.source.light.adapter.teaching.facade.impl.CourseFacadeImpl;
import top.egon.cola.archetype.source.light.adapter.teaching.facade.impl.SchoolClassFacadeImpl;
import top.egon.cola.archetype.source.light.application.teaching.manage.CourseManage;
import top.egon.cola.archetype.source.light.application.teaching.manage.SchoolClassManage;
import top.egon.cola.archetype.source.light.application.teaching.pojo.command.CreateCourseCommand;
import top.egon.cola.archetype.source.light.application.teaching.pojo.query.GetCourseQuery;
import top.egon.cola.archetype.source.light.application.teaching.pojo.query.GetSchoolClassQuery;
import top.egon.cola.archetype.source.light.application.teaching.pojo.result.CourseResult;
import top.egon.cola.archetype.source.light.application.teaching.pojo.result.SchoolClassResult;
import top.egon.cola.archetype.source.light.common.exception.TeachingUseCaseException;
import top.egon.cola.archetype.source.light.facade.proto.CourseRpcResponse;
import top.egon.cola.archetype.source.light.facade.proto.CreateCourseRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.GetCourseRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.GetSchoolClassRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.SchoolClassRpcResponse;
import top.egon.cola.archetype.source.light.facade.teaching.dto.CourseDTO;
import top.egon.cola.archetype.source.light.facade.teaching.dto.SchoolClassDetailDTO;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The single Protobuf chain replaces the former DTO facade plus RPC provider wrapper. */
class TeachingFacadeImplTest {

    private final LightFacadeConverter converter = Mappers.getMapper(LightFacadeConverter.class);
    private final jakarta.validation.ValidatorFactory validators = Validation.buildDefaultValidatorFactory();
    private final ValidationUtils validation = new ValidationUtils(validators.getValidator());
    private final CourseManage courseManage = mock(CourseManage.class);
    private final SchoolClassManage schoolClassManage = mock(SchoolClassManage.class);
    private final CourseFacadeImpl courseFacade = new CourseFacadeImpl(courseManage, converter, validation);
    private final SchoolClassFacadeImpl schoolClassFacade =
            new SchoolClassFacadeImpl(schoolClassManage, converter, validation);

    @AfterEach
    void closeValidationFactory() {
        validators.close();
    }

    @Test
    void maps_create_course_request_onto_the_use_case() {
        when(courseManage.create(any())).thenReturn(new CourseResult(1002L, "MATH", "Math", "ACTIVE"));

        CourseRpcResponse response = courseFacade.createCourse(CreateCourseRpcRequest.newBuilder()
                .setCode("MATH")
                .setName("Math")
                .setOperatorId("operator-1")
                .setRequestId("request-1")
                .build());

        assertThat(response.getSuccess()).isTrue();
        assertThat(converter.toSource(response.getData()))
                .isEqualTo(new CourseDTO(1002L, "MATH", "Math", "ACTIVE"));
        verify(courseManage).create(new CreateCourseCommand("MATH", "Math", "operator-1", "request-1"));
    }

    @Test
    void maps_application_failure_onto_the_wire_code_without_leaking_the_cause() {
        when(courseManage.create(any())).thenThrow(new TeachingUseCaseException(
                "COURSE_EXISTS", "Course exists", new IllegalStateException("internal")));

        CourseRpcResponse response = courseFacade.createCourse(CreateCourseRpcRequest.newBuilder()
                .setCode("MATH")
                .setName("Math")
                .setOperatorId("operator-1")
                .setRequestId("request-1")
                .build());

        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo("COURSE_EXISTS");
        assertThat(response.getMessage()).isEqualTo("Course exists");
    }

    @Test
    void delegates_course_query_to_the_use_case() {
        when(courseManage.get(any())).thenReturn(new CourseResult(1002L, "MATH", "Math", "ACTIVE"));

        CourseRpcResponse response = courseFacade.getCourse(
                GetCourseRpcRequest.newBuilder().setCourseId(1002L).build());

        assertThat(response.getSuccess()).isTrue();
        verify(courseManage).get(new GetCourseQuery(1002L));
    }

    @Test
    void rejects_an_absent_identifier_before_reaching_the_use_case() {
        assertThatThrownBy(() -> courseFacade.getCourse(GetCourseRpcRequest.newBuilder().build()))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void delegates_school_class_query_to_the_use_case() {
        when(schoolClassManage.get(any())).thenReturn(
                new SchoolClassResult(1003L, "Class One", "2026-FALL", "ACTIVE", 2));

        SchoolClassRpcResponse response = schoolClassFacade.getSchoolClass(
                GetSchoolClassRpcRequest.newBuilder().setSchoolClassId(1003L).build());

        assertThat(response.getSuccess()).isTrue();
        assertThat(converter.toSource(response.getData()))
                .isEqualTo(new SchoolClassDetailDTO(1003L, "Class One", "2026-FALL", "ACTIVE", 2));
        verify(schoolClassManage).get(new GetSchoolClassQuery(1003L));
    }
}
