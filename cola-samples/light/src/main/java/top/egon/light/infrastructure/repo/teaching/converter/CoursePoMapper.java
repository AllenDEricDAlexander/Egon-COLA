package top.egon.light.infrastructure.repo.teaching.converter;

import top.egon.light.domain.teaching.model.Course;
import top.egon.light.infrastructure.repo.teaching.po.CoursePo;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CoursePoMapper extends BaseMapper<Course, CoursePo> {
    @Override
    @Mapping(target = "createdAt", ignore = true)
    CoursePo convert(Course course);
}
