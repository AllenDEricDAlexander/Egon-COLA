#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.course.repos;

import java.util.Optional;

import ${package}.domain.common.Page;
import ${package}.domain.course.entities.Course;
import ${package}.domain.course.vos.CourseCode;
import ${package}.domain.course.vos.CourseId;

public interface CourseRepository {

    Course save(Course course);

    Optional<Course> findById(CourseId courseId);

    Optional<Course> findByCode(CourseCode courseCode);

    Page<Course> findPage(int currentPage, int pageSize);

    boolean existsByCode(CourseCode courseCode);
}
