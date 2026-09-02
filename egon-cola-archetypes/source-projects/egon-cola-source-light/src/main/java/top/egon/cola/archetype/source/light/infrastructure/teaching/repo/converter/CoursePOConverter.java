package top.egon.cola.archetype.source.light.infrastructure.teaching.repo.converter;

import top.egon.cola.archetype.source.light.domain.teaching.entities.Course;
import top.egon.cola.archetype.source.light.domain.teaching.enums.CourseStatus;
import top.egon.cola.archetype.source.light.domain.teaching.vos.CourseCode;
import top.egon.cola.archetype.source.light.infrastructure.teaching.repo.po.CoursePO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;

/** MapStruct conversion between the course domain entity and its PO. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CoursePOConverter extends BaseConverter<Course, CoursePO> {

    @Override
    @Mapping(target = "courseCode", expression = "java(source.code().value())")
    @Mapping(target = "name", expression = "java(source.name())")
    @Mapping(target = "status", expression = "java(source.status().name())")
    CoursePO toTarget(Course source);

    @Override
    default Course toSource(CoursePO target) {
        return new Course(target.getId(), new CourseCode(target.getCourseCode()), target.getName(),
                CourseStatus.valueOf(target.getStatus()));
    }
}
