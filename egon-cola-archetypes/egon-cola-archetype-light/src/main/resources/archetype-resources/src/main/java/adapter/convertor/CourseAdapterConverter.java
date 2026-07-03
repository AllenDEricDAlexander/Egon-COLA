package ${package}.adapter.convertor;

import ${package}.domain.teaching.model.Course;
import ${package}.facade.dto.CourseDTO;
import io.github.linpeilie.BaseMapper;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("courseAdapterConverter")
@RequiredArgsConstructor
public class CourseAdapterConverter {
    @Qualifier("converter")
    private final Converter converter;

    public CourseDTO toDto(Course course) {
        return converter.convert(course, CourseDTO.class);
    }

    @Mapper(componentModel = "spring")
    public interface CourseMapper extends BaseMapper<Course, CourseDTO> {
    }
}
