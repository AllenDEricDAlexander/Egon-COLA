package ${package}.adapter.convertor;

import ${package}.domain.teaching.model.Course;
import ${package}.facade.dto.CourseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("courseAdapterConverter")
@RequiredArgsConstructor
public class CourseAdapterConverter {
    @Qualifier("courseAdapterMapperImpl")
    private final CourseAdapterMapper courseAdapterMapper;

    public CourseDTO toDto(Course course) {
        return courseAdapterMapper.convert(course);
    }
}
