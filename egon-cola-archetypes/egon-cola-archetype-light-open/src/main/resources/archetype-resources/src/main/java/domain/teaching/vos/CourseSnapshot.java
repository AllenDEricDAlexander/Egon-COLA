package ${package}.domain.teaching.vos;

import ${package}.domain.teaching.entities.Course;
import ${package}.domain.teaching.enums.CourseStatus;

public record CourseSnapshot(long id, CourseCode code, String name, CourseStatus status) {
    public static CourseSnapshot from(Course course) {
        return new CourseSnapshot(course.id(), course.code(), course.name(), course.status());
    }
}
