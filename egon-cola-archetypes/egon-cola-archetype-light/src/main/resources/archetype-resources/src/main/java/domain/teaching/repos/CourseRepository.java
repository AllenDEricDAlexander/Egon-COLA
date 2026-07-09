package ${package}.domain.teaching.repos;

import ${package}.domain.teaching.model.Course;
import ${package}.domain.teaching.vos.CourseCode;

import java.util.Optional;

public interface CourseRepository {
    default Course save(Course course) {
        throw new UnsupportedOperationException("legacy repository method");
    }

    default Optional<Course> findLegacyById(String courseId) {
        throw new UnsupportedOperationException("legacy repository method");
    }

    default ${package}.domain.teaching.entities.Course save(
            ${package}.domain.teaching.entities.Course course) {
        throw new UnsupportedOperationException("not implemented");
    }

    default Optional<${package}.domain.teaching.entities.Course> findById(String courseId) {
        throw new UnsupportedOperationException("not implemented");
    }

    default Optional<${package}.domain.teaching.entities.Course> findByCode(CourseCode courseCode) {
        throw new UnsupportedOperationException("not implemented");
    }
}
