package top.egon.cola.archetype.source.light.application.teaching.pojo.convertor;

import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.light.application.teaching.pojo.command.ScheduleCourseCommand;
import top.egon.cola.archetype.source.light.application.teaching.pojo.result.CourseResult;
import top.egon.cola.archetype.source.light.application.teaching.pojo.result.SchoolClassResult;
import top.egon.cola.archetype.source.light.domain.teaching.aggregates.SchoolClassAggregate;
import top.egon.cola.archetype.source.light.domain.teaching.entities.Course;
import top.egon.cola.archetype.source.light.domain.teaching.entities.SchoolClass;
import top.egon.cola.archetype.source.light.domain.teaching.vos.CourseSchedule;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;

/** Teaching entity-to-result projection. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
@AnnotateWith(value = Component.class, elements = @AnnotateWith.Element(strings = "teachingApplicationConvertorImpl"))
public interface TeachingApplicationConvertor extends BaseForwardConverter<Course, CourseResult> {

    @Override
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code.value")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "status", source = "status")
    CourseResult toTarget(Course source);

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "semester", source = "semester.value")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "scheduleCount", constant = "0")
    SchoolClassResult toSchoolClassResult(SchoolClass source);

    // The aggregate keeps record-style accessors, so MapStruct reads it through expressions.
    @Mapping(target = "id", expression = "java(source.schoolClass().id().value())")
    @Mapping(target = "name", expression = "java(source.schoolClass().name())")
    @Mapping(target = "semester", expression = "java(source.schoolClass().semester().value())")
    @Mapping(target = "status", expression = "java(source.schoolClass().status().name())")
    @Mapping(target = "scheduleCount", expression = "java(source.schedules().size())")
    SchoolClassResult toSchoolClassResult(SchoolClassAggregate source);

    @Mapping(target = "courseCode", source = "course.code")
    @Mapping(target = "startsAt", source = "command.startsAt")
    @Mapping(target = "endsAt", source = "command.endsAt")
    CourseSchedule toSchedule(ScheduleCourseCommand command, Course course);
}
