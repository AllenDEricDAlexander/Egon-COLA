package ${package}.infrastructure.repo.teaching.converter;

import ${package}.domain.teaching.model.Course;
import ${package}.infrastructure.repo.teaching.po.CoursePo;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CoursePoMapper extends BaseMapper<Course, CoursePo> {
    @Override
    @Mapping(target = "createdAt", ignore = true)
    CoursePo convert(Course course);
}
