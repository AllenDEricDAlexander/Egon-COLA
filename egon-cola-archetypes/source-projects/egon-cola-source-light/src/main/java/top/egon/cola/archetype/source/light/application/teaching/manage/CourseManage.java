package top.egon.cola.archetype.source.light.application.teaching.manage;

import top.egon.cola.archetype.source.light.application.teaching.pojo.command.CreateCourseCommand;
import top.egon.cola.archetype.source.light.application.teaching.pojo.query.GetCourseQuery;
import top.egon.cola.archetype.source.light.application.teaching.pojo.result.CourseResult;

public interface CourseManage {
    CourseResult create(CreateCourseCommand command);

    CourseResult get(GetCourseQuery query);
}
