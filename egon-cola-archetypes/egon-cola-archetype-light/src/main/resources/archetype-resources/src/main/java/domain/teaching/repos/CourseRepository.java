package ${package}.domain.teaching.repos;

import ${package}.domain.teaching.model.Course;

import java.util.Optional;

public interface CourseRepository {
    Course save(Course course);
    Optional<Course> findById(String courseId);
}
