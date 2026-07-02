package top.egon.fable.domain.repos.course;

import java.util.Optional;

import top.egon.fable.domain.entities.course.Course;

public interface CourseRepository {

    Course save(Course course);

    Optional<Course> findById(String courseId);

    boolean existsByName(String name);
}
