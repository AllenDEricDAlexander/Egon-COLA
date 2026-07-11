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
import ${package}.application.result.PageResult;

public interface CourseManage {

    CourseResult create(CreateCourseCommand command);

    CourseScheduleResult schedule(ScheduleCourseCommand command);

    CourseResult get(GetCourseQuery query);

    PageResult<CourseResult> page(PageCourseQuery query);
}
