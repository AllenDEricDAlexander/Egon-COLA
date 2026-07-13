#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.course.manage;

import ${package}.application.course.command.CreateCourseCommand;
import ${package}.application.course.command.ScheduleCourseCommand;
import ${package}.application.course.query.GetCourseQuery;
import ${package}.application.course.query.PageCourseQuery;
import ${package}.application.course.result.CourseResult;
import ${package}.application.course.result.CourseScheduleResult;
import ${package}.application.result.PageResult;

public interface CourseManage {

    CourseResult create(CreateCourseCommand command);

    CourseScheduleResult schedule(ScheduleCourseCommand command);

    CourseResult get(GetCourseQuery query);

    PageResult<CourseResult> page(PageCourseQuery query);
}
