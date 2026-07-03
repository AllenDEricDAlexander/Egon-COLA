#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.convertor;

import ${package}.domain.entities.course.Course;
import ${package}.domain.enums.CourseStatus;
import ${package}.facade.dto.course.CourseDTO;
import io.github.linpeilie.BaseMapper;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("courseAdapterConvertor")
@RequiredArgsConstructor
public class CourseAdapterConvertor {

    @Qualifier("converter")
    private final Converter converter;

    public CourseDTO toDTO(Course course) {
        CourseDTO courseDTO = converter.convert(course, CourseDTO.class);
        courseDTO.setStatus(toFacadeStatus(course.getStatus()));
        return courseDTO;
    }

    private String toFacadeStatus(CourseStatus status) {
        if (CourseStatus.ACTIVE == status) {
            return "ENABLED";
        }
        return status.name();
    }

    @Mapper(componentModel = "spring")
    public interface CourseMapper extends BaseMapper<Course, CourseDTO> {

        @Override
        @Mapping(target = "status", ignore = true)
        CourseDTO convert(Course course);
    }
}
