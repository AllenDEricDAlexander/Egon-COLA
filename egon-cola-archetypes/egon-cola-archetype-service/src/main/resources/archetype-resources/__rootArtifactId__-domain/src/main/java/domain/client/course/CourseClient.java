#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.client.course;

import java.util.Optional;

import ${package}.domain.common.Page;
import ${package}.domain.entities.course.Course;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public interface CourseClient {

    Course save(@NotNull Course course);

    Optional<Course> findById(@NotBlank String courseId);

    Page<Course> findPage(@Positive int currentPage, @Positive int pageSize);

    boolean existsByName(@NotBlank String name);
}
