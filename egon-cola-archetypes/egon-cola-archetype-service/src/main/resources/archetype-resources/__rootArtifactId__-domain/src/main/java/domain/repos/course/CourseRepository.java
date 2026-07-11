#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.repos.course;

import java.util.Optional;

import ${package}.domain.common.Page;
import ${package}.domain.entities.course.Course;
import ${package}.domain.vos.course.CourseCode;
import ${package}.domain.vos.course.CourseId;

public interface CourseRepository {

    Course save(Course course);

    Optional<Course> findById(String courseId);

    default Optional<Course> findById(CourseId courseId) {
        return findById(courseId.value());
    }

    default Optional<Course> findByCode(CourseCode courseCode) {
        return Optional.empty();
    }

    Page<Course> findPage(int currentPage, int pageSize);

    boolean existsByName(String name);

    default boolean existsByCode(CourseCode courseCode) {
        return findByCode(courseCode).isPresent();
    }
}
