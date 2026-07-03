package top.egon.light.adapter.convertor;

import top.egon.light.domain.teaching.model.Course;
import top.egon.light.facade.dto.CourseDTO;
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
