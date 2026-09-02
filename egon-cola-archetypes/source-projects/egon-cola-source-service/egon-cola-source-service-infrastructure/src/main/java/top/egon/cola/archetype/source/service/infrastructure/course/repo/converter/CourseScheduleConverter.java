package top.egon.cola.archetype.source.service.infrastructure.course.repo.converter;

import top.egon.cola.archetype.source.service.domain.course.entities.CourseSchedule;
import top.egon.cola.archetype.source.service.domain.course.enums.CourseScheduleStatus;
import top.egon.cola.archetype.source.service.domain.course.vos.CourseId;
import top.egon.cola.archetype.source.service.infrastructure.course.repo.po.CourseSchedulePO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CourseScheduleConverter extends BaseConverter<CourseSchedule, CourseSchedulePO> {

    @Override
    @Mapping(target = "courseId", expression = "java(source.getCourseId().value())")
    @Mapping(target = "status", expression = "java(source.getStatus().name())")
    CourseSchedulePO toTarget(CourseSchedule source);

    @Override
    default CourseSchedule toSource(CourseSchedulePO target) {
        return new CourseSchedule(
                target.getId(), new CourseId(target.getCourseId()), target.getClassId(),
                target.getStartsAt(), target.getEndsAt(),
                CourseScheduleStatus.valueOf(target.getStatus()));
    }
}
