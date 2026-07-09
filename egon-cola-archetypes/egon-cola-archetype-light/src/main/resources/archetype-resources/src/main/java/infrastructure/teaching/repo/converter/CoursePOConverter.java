package ${package}.infrastructure.teaching.repo.converter;

import ${package}.domain.teaching.entities.Course;
import ${package}.domain.teaching.enums.CourseStatus;
import ${package}.domain.teaching.vos.CourseCode;
import ${package}.infrastructure.teaching.repo.po.CoursePO;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class CoursePOConverter {
    public CoursePO toPO(Course course) {
        return new CoursePO(
                course.id(), course.code().value(), course.name(), course.status().name(), Instant.now());
    }

    public Course toDomain(CoursePO course) {
        return new Course(
                course.getId(), new CourseCode(course.getCourseCode()), course.getName(),
                CourseStatus.valueOf(course.getStatus()));
    }
}
