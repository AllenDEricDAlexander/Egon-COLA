package top.egon.fable.infrastructure.repo.course.converter;

import top.egon.fable.domain.entities.course.Course;
import top.egon.fable.infrastructure.repo.course.po.CoursePo;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CoursePoMapper extends BaseMapper<Course, CoursePo> {

    @Override
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    CoursePo convert(Course course);

    @Override
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    CoursePo convert(Course course, @MappingTarget CoursePo coursePo);
}
