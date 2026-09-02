package top.egon.cola.archetype.source.light.application.teaching.manage;

import top.egon.cola.archetype.source.light.application.teaching.command.CreateCourseCommand;
import top.egon.cola.archetype.source.light.application.teaching.query.GetCourseQuery;
import top.egon.cola.archetype.source.light.application.teaching.result.CourseResult;

public interface CourseManage {
    CourseResult create(CreateCourseCommand command);

    CourseResult get(GetCourseQuery query);
}
