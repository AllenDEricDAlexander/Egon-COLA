package ${package}.application.manage.teaching;

import ${package}.application.command.teaching.CreateGradeCommand;
import ${package}.application.query.teaching.GradeDetailQuery;
import ${package}.application.result.teaching.GradeDetailResult;

public interface GradeManage {
    GradeDetailResult createGrade(CreateGradeCommand command);
    GradeDetailResult getGrade(GradeDetailQuery query);
}
