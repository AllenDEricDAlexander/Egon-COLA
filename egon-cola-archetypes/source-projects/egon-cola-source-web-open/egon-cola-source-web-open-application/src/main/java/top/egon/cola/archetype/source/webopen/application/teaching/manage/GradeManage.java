package top.egon.cola.archetype.source.webopen.application.teaching.manage;

import top.egon.cola.archetype.source.webopen.application.teaching.command.CreateGradeCommand;
import top.egon.cola.archetype.source.webopen.application.teaching.query.GradeDetailQuery;
import top.egon.cola.archetype.source.webopen.application.teaching.result.GradeDetailResult;

public interface GradeManage {
    GradeDetailResult createGrade(CreateGradeCommand command);
    GradeDetailResult getGrade(GradeDetailQuery query);
}
