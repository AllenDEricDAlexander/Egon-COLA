#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.repo.course.converter;

import ${package}.domain.course.entities.Course;
import ${package}.infrastructure.repo.course.po.CoursePo;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CourseDomainMapper extends BaseMapper<CoursePo, Course> {

    @Override
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "status", ignore = true)
    Course convert(CoursePo coursePo);

    @Override
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "status", ignore = true)
    Course convert(CoursePo coursePo, @MappingTarget Course course);
}
