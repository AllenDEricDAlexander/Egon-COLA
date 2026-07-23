package ${package}.infrastructure.teaching.repo.converter;

import ${package}.domain.teaching.entities.Course;
import ${package}.domain.teaching.enums.CourseStatus;
import ${package}.domain.teaching.vos.CourseCode;
import ${package}.infrastructure.teaching.repo.po.CoursePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CoursePOConverter {

    private final CoursePOMapper mapper;

    public CoursePO toPO(Course course) {
        return mapper.convert(course);
    }

    public Course toDomain(CoursePO course) {
        return new Course(
                course.getId(), new CourseCode(course.getCourseCode()), course.getName(),
                CourseStatus.valueOf(course.getStatus()));
    }
}
