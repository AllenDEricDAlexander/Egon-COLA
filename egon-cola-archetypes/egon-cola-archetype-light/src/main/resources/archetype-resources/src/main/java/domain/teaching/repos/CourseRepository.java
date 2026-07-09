package ${package}.domain.teaching.repos;

import ${package}.domain.teaching.entities.Course;
import ${package}.domain.teaching.vos.CourseCode;

import java.util.Optional;

public interface CourseRepository {
    Course save(Course course);

    Optional<Course> findById(String courseId);

    Optional<Course> findByCode(CourseCode courseCode);
}
