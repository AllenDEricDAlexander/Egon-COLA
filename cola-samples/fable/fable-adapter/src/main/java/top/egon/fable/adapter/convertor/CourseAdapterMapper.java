package top.egon.fable.adapter.convertor;

import top.egon.fable.domain.entities.course.Course;
import top.egon.fable.facade.dto.course.CourseDTO;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CourseAdapterMapper extends BaseMapper<Course, CourseDTO> {
    @Override
    @Mapping(target = "status", ignore = true)
    CourseDTO convert(Course course);
}
