package top.egon.cola.archetype.source.lightopen.application.teaching.manage;

import top.egon.cola.archetype.source.lightopen.application.teaching.command.CreateSchoolClassCommand;
import top.egon.cola.archetype.source.lightopen.application.teaching.command.ScheduleCourseCommand;
import top.egon.cola.archetype.source.lightopen.application.teaching.query.GetSchoolClassQuery;
import top.egon.cola.archetype.source.lightopen.application.teaching.result.SchoolClassResult;

public interface SchoolClassManage {
    SchoolClassResult create(CreateSchoolClassCommand command);

    SchoolClassResult schedule(ScheduleCourseCommand command);

    SchoolClassResult get(GetSchoolClassQuery query);
}
