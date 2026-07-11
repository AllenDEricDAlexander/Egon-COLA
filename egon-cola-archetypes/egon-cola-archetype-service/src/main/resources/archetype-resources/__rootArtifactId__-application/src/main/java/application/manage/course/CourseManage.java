#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.manage.course;

import ${package}.application.command.course.CreateCourseCommand;
import ${package}.application.command.course.ScheduleCourseCommand;
import ${package}.application.query.course.GetCourseQuery;
import ${package}.application.query.course.PageCourseQuery;
import ${package}.application.result.course.CourseResult;
import ${package}.application.result.course.CourseScheduleResult;
import ${package}.domain.common.Page;
import ${package}.domain.entities.course.Course;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public interface CourseManage {

    CourseResult create(CreateCourseCommand command);

    CourseScheduleResult schedule(ScheduleCourseCommand command);

    CourseResult get(GetCourseQuery query);

    Page<CourseResult> page(PageCourseQuery query);

    Course create(@NotBlank String name, @Positive int credit);

    Course getById(@NotBlank String courseId);

    Page<Course> getPage(@Positive int currentPage, @Positive int pageSize);
}
