package ${package}.application.teaching.manage;

import ${package}.application.teaching.command.CreateCourseCommand;
import ${package}.application.teaching.query.GetCourseQuery;
import ${package}.application.teaching.result.CourseResult;

public interface CourseManage {
    CourseResult create(CreateCourseCommand command);

    CourseResult get(GetCourseQuery query);
}
