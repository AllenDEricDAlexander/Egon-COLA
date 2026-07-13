#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.course.repo.converter;

import ${package}.domain.course.entities.Course;
import ${package}.infrastructure.course.repo.po.CoursePo;
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
    @Mapping(target = "code", ignore = true)
    CoursePo convert(Course course);

    @Override
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "code", ignore = true)
    CoursePo convert(Course course, @MappingTarget CoursePo coursePo);
}
