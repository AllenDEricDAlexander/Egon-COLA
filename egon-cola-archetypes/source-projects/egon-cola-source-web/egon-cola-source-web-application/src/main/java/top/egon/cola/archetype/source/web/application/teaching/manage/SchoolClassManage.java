package top.egon.cola.archetype.source.web.application.teaching.manage;

import top.egon.cola.archetype.source.web.application.teaching.command.CreateSchoolClassCommand;
import top.egon.cola.archetype.source.web.application.teaching.command.AssignUserToClassCommand;
import top.egon.cola.archetype.source.web.application.teaching.query.SchoolClassDetailQuery;
import top.egon.cola.archetype.source.web.application.teaching.result.SchoolClassDetailResult;

public interface SchoolClassManage {
    SchoolClassDetailResult createSchoolClass(CreateSchoolClassCommand command);
    SchoolClassDetailResult getSchoolClass(SchoolClassDetailQuery query);

    void assignUser(AssignUserToClassCommand command);
}
