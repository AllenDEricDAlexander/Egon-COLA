package top.egon.light.domain.teaching.repos;

import top.egon.light.domain.teaching.model.Course;

import java.util.Optional;

public interface CourseRepository {
    Course save(Course course);

    Optional<Course> findById(String courseId);
}
