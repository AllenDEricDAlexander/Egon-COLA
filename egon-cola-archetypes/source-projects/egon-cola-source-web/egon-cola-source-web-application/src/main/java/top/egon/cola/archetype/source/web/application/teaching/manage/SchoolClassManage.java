package top.egon.cola.archetype.source.web.application.teaching.manage;

import top.egon.cola.archetype.source.web.application.teaching.pojo.command.CreateSchoolClassCommand;
import top.egon.cola.archetype.source.web.application.teaching.pojo.command.AssignUserToClassCommand;
import top.egon.cola.archetype.source.web.application.teaching.pojo.query.SchoolClassDetailQuery;
import top.egon.cola.archetype.source.web.application.teaching.pojo.result.SchoolClassDetailResult;

public interface SchoolClassManage {
    SchoolClassDetailResult createSchoolClass(CreateSchoolClassCommand command);
    SchoolClassDetailResult getSchoolClass(SchoolClassDetailQuery query);

    void assignUser(AssignUserToClassCommand command);
}
