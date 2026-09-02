package top.egon.cola.archetype.source.service.adapter.course.converter;

import top.egon.cola.archetype.source.service.application.course.command.CreateCourseCommand;
import top.egon.cola.archetype.source.service.application.course.command.ScheduleCourseCommand;
import top.egon.cola.archetype.source.service.application.course.result.CourseResult;
import top.egon.cola.archetype.source.service.application.course.result.CourseScheduleResult;
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
