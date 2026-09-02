package top.egon.cola.archetype.source.serviceopen.infrastructure.course.repo.converter;

import top.egon.cola.archetype.source.serviceopen.domain.course.entities.Course;
import top.egon.cola.archetype.source.serviceopen.domain.course.enums.CourseStatus;
import top.egon.cola.archetype.source.serviceopen.domain.course.vos.CourseCode;
import top.egon.cola.archetype.source.serviceopen.infrastructure.course.repo.po.CoursePO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CourseConverter extends BaseConverter<Course, CoursePO> {

    @Override
    @Mapping(target = "code", expression = "java(source.getCode().value())")
    @Mapping(target = "status", expression = "java(source.getStatus().name())")
    CoursePO toTarget(Course source);

    @Override
    default Course toSource(CoursePO target) {
        Course course = new Course();
        course.setId(target.getId());
        course.setCode(new CourseCode(target.getCode()));
        course.setName(target.getName());
        course.setCredit(target.getCredit());
        course.setStatus(CourseStatus.valueOf(target.getStatus()));
        return course;
    }
}
