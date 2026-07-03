package ${package}.adapter.convertor;

import ${package}.domain.teaching.model.Course;
import ${package}.facade.dto.CourseDTO;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CourseAdapterMapper extends BaseMapper<Course, CourseDTO> {
}
