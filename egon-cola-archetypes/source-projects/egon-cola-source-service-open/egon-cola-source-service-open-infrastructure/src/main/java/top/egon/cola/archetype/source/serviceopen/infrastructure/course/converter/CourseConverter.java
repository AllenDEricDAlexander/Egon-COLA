package top.egon.cola.archetype.source.serviceopen.infrastructure.course.converter;

import top.egon.cola.archetype.source.serviceopen.domain.course.entities.Course;
import top.egon.cola.archetype.source.serviceopen.domain.course.enums.CourseStatus;
import top.egon.cola.archetype.source.serviceopen.domain.course.vos.CourseCode;
import top.egon.cola.archetype.source.serviceopen.infrastructure.course.po.CoursePO;
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
public interface CourseConverter extends BaseConverter<Course, CoursePO> {

    @Override
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "code", expression = "java(source.getCode().value())")
    @Mapping(target = "status", expression = "java(source.getStatus().name())")
    CoursePO toTarget(Course source);

    @Override
    @BeanMapping(ignoreByDefault = true, qualifiedByName = "restoreDomain")
    Course toSource(CoursePO target);

    @ObjectFactory
    @Named("restoreDomain")
    default Course restoreDomain(CoursePO target) {
        Course course = new Course();
        course.setId(target.getId());
        course.setCode(new CourseCode(target.getCode()));
        course.setName(target.getName());
        course.setCredit(target.getCredit());
        course.setStatus(CourseStatus.valueOf(target.getStatus()));
        return course;
    }
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "createUserId", source = "createUserId")
    @Mapping(target = "createTime", source = "createTime")
    @Mapping(target = "version", source = "version")
    void updateMetadata(@MappingTarget CoursePO target, CoursePO source);
}
