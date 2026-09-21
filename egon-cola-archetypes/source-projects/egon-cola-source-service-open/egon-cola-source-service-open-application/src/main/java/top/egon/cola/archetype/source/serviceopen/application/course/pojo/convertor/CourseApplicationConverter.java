package top.egon.cola.archetype.source.serviceopen.application.course.pojo.convertor;

import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.serviceopen.application.course.pojo.result.CourseResult;
import top.egon.cola.archetype.source.serviceopen.application.course.pojo.result.CourseScheduleResult;
import top.egon.cola.archetype.source.serviceopen.domain.course.entities.Course;
import top.egon.cola.archetype.source.serviceopen.domain.course.entities.CourseSchedule;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;

/** Course entity-to-result projection; the value objects are read through their record accessors. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
@AnnotateWith(value = Component.class, elements = @AnnotateWith.Element(strings = "courseApplicationConverterImpl"))
public interface CourseApplicationConverter extends BaseForwardConverter<Course, CourseResult> {

    @Override
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code.value")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "credit", source = "credit")
    @Mapping(target = "status", source = "status")
    CourseResult toTarget(Course source);

    /** Callable contract kept from the previous hand-written projection. */
    default CourseResult toResult(Course source) {
        return toTarget(source);
    }

    @Mapping(target = "id", source = "id")
    @Mapping(target = "courseId", source = "courseId.value")
    @Mapping(target = "classId", source = "classId")
    @Mapping(target = "startsAt", source = "startsAt")
    @Mapping(target = "endsAt", source = "endsAt")
    @Mapping(target = "status", source = "status")
    CourseScheduleResult toResult(CourseSchedule source);
}
