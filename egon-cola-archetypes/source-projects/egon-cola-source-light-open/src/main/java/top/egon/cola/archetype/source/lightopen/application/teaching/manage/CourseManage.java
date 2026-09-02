package top.egon.cola.archetype.source.lightopen.application.teaching.manage;

import top.egon.cola.archetype.source.lightopen.application.teaching.command.CreateCourseCommand;
import top.egon.cola.archetype.source.lightopen.application.teaching.query.GetCourseQuery;
import top.egon.cola.archetype.source.lightopen.application.teaching.result.CourseResult;

public interface CourseManage {
    CourseResult create(CreateCourseCommand command);

    CourseResult get(GetCourseQuery query);
}
