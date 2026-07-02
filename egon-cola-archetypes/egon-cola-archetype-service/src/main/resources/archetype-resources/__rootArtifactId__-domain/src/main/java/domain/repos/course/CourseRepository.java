#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.repos.course;

import java.util.Optional;

import ${package}.domain.entities.course.Course;

public interface CourseRepository {

    Course save(Course course);

    Optional<Course> findById(String courseId);

    boolean existsByName(String name);
}
