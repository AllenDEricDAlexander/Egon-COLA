package top.egon.cola.archetype.source.light.application.teaching.manage;

import top.egon.cola.archetype.source.light.application.teaching.pojo.command.CreateSchoolClassCommand;
import top.egon.cola.archetype.source.light.application.teaching.pojo.command.ScheduleCourseCommand;
import top.egon.cola.archetype.source.light.application.teaching.pojo.query.GetSchoolClassQuery;
import top.egon.cola.archetype.source.light.application.teaching.pojo.result.SchoolClassResult;

public interface SchoolClassManage {
    SchoolClassResult create(CreateSchoolClassCommand command);

    SchoolClassResult schedule(ScheduleCourseCommand command);

    SchoolClassResult get(GetSchoolClassQuery query);
}
