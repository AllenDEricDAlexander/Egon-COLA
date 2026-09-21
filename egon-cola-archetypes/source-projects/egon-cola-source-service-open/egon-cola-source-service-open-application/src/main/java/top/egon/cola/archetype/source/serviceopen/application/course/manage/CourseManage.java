package top.egon.cola.archetype.source.serviceopen.application.course.manage;

import top.egon.cola.archetype.source.serviceopen.application.course.pojo.command.CreateCourseCommand;
import top.egon.cola.archetype.source.serviceopen.application.course.pojo.command.ScheduleCourseCommand;
import top.egon.cola.archetype.source.serviceopen.application.course.pojo.query.GetCourseQuery;
import top.egon.cola.archetype.source.serviceopen.application.course.pojo.query.PageCourseQuery;
import top.egon.cola.archetype.source.serviceopen.application.course.pojo.result.CourseResult;
import top.egon.cola.archetype.source.serviceopen.application.course.pojo.result.CourseScheduleResult;
import top.egon.cola.archetype.source.serviceopen.application.pojo.result.PageResult;

public interface CourseManage {

    CourseResult create(CreateCourseCommand command);

    CourseScheduleResult schedule(ScheduleCourseCommand command);

    CourseResult get(GetCourseQuery query);

    PageResult<CourseResult> page(PageCourseQuery query);
}
