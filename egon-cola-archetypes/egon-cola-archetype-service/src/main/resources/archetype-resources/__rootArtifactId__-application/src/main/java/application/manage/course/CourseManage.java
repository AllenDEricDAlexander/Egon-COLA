#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.manage.course;

import ${package}.domain.common.Page;
import ${package}.domain.entities.course.Course;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public interface CourseManage {

    Course create(@NotBlank String name, @Positive int credit);

    Course getById(@NotBlank String courseId);

    Page<Course> getPage(@Positive int currentPage, @Positive int pageSize);
}
