#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.convertor;

import ${package}.domain.entities.course.Course;
import ${package}.facade.dto.course.CourseDTO;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CourseAdapterMapper extends BaseMapper<Course, CourseDTO> {
    @Override
    @Mapping(target = "status", ignore = true)
    CourseDTO convert(Course course);
}
