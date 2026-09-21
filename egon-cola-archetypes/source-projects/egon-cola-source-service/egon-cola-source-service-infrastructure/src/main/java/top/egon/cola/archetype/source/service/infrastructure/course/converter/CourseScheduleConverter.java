package top.egon.cola.archetype.source.service.infrastructure.course.converter;

import top.egon.cola.archetype.source.service.domain.course.entities.CourseSchedule;
import top.egon.cola.archetype.source.service.domain.course.enums.CourseScheduleStatus;
import top.egon.cola.archetype.source.service.domain.course.vos.CourseId;
import top.egon.cola.archetype.source.service.infrastructure.course.po.CourseSchedulePO;
import org.mapstruct.Mapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ObjectFactory;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CourseScheduleConverter extends BaseConverter<CourseSchedule, CourseSchedulePO> {

    @Override
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "courseId", expression = "java(source.getCourseId().value())")
    @Mapping(target = "status", expression = "java(source.getStatus().name())")
    CourseSchedulePO toTarget(CourseSchedule source);

    @Override
    @BeanMapping(ignoreByDefault = true, qualifiedByName = "restoreDomain")
    CourseSchedule toSource(CourseSchedulePO target);

    @ObjectFactory
    @Named("restoreDomain")
    default CourseSchedule restoreDomain(CourseSchedulePO target) {
        return new CourseSchedule(
                target.getId(), new CourseId(target.getCourseId()), target.getClassId(),
                target.getStartsAt(), target.getEndsAt(),
                CourseScheduleStatus.valueOf(target.getStatus()));
    }
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "createUserId", source = "createUserId")
    @Mapping(target = "createTime", source = "createTime")
    @Mapping(target = "version", source = "version")
    void updateMetadata(@MappingTarget CourseSchedulePO target, CourseSchedulePO source);
}
