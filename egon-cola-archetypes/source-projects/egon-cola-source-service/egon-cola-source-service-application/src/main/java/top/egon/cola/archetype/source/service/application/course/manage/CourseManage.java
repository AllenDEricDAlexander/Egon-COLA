package top.egon.cola.archetype.source.service.application.course.manage;

import top.egon.cola.archetype.source.service.application.course.command.CreateCourseCommand;
import top.egon.cola.archetype.source.service.application.course.command.ScheduleCourseCommand;
import top.egon.cola.archetype.source.service.application.course.query.GetCourseQuery;
import top.egon.cola.archetype.source.service.application.course.query.PageCourseQuery;
import top.egon.cola.archetype.source.service.application.course.result.CourseResult;
import top.egon.cola.archetype.source.service.application.course.result.CourseScheduleResult;
import top.egon.cola.archetype.source.service.application.result.PageResult;

public interface CourseManage {

    CourseResult create(CreateCourseCommand command);

    CourseScheduleResult schedule(ScheduleCourseCommand command);

    CourseResult get(GetCourseQuery query);

    PageResult<CourseResult> page(PageCourseQuery query);
}
