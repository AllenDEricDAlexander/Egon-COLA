#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.course.converter;

import ${package}.application.course.command.CreateCourseCommand;
import ${package}.application.course.command.ScheduleCourseCommand;
import ${package}.application.course.result.CourseResult;
import ${package}.application.course.result.CourseScheduleResult;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.evaluation.facade.course.dto.CourseResponse;
import top.egon.cola.evaluation.facade.course.dto.CourseScheduleResponse;
import top.egon.cola.evaluation.facade.course.dto.CreateCourseRequest;
import top.egon.cola.evaluation.facade.course.dto.ScheduleCourseRequest;

import java.util.Objects;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CourseFacadeConverter {

    CreateCourseCommand toCommand(CreateCourseRequest request);

    ScheduleCourseCommand toCommand(ScheduleCourseRequest request);

    CourseResponse toResponse(CourseResult result);

    CourseScheduleResponse toResponse(CourseScheduleResult result);

    @BeforeMapping
    default void requireCreateRequest(CreateCourseRequest request) {
        Objects.requireNonNull(request, "request");
    }

    @BeforeMapping
    default void requireScheduleRequest(ScheduleCourseRequest request) {
        Objects.requireNonNull(request, "request");
    }

    @BeforeMapping
    default void requireCourseResult(CourseResult result) {
        Objects.requireNonNull(result, "result");
    }

    @BeforeMapping
    default void requireScheduleResult(CourseScheduleResult result) {
        Objects.requireNonNull(result, "result");
    }
}
