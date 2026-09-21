package top.egon.cola.archetype.source.webopen.application.teaching.manage;

import top.egon.cola.archetype.source.webopen.application.teaching.pojo.command.CreateGradeCommand;
import top.egon.cola.archetype.source.webopen.application.teaching.pojo.query.GradeDetailQuery;
import top.egon.cola.archetype.source.webopen.application.teaching.pojo.result.GradeDetailResult;

public interface GradeManage {
    GradeDetailResult createGrade(CreateGradeCommand command);
    GradeDetailResult getGrade(GradeDetailQuery query);
}
