package ${package}.application.teaching.manage;

import ${package}.application.teaching.command.CreateSchoolClassCommand;
import ${package}.application.teaching.command.AssignUserToClassCommand;
import ${package}.application.teaching.query.SchoolClassDetailQuery;
import ${package}.application.teaching.result.SchoolClassDetailResult;

public interface SchoolClassManage {
    SchoolClassDetailResult createSchoolClass(CreateSchoolClassCommand command);
    SchoolClassDetailResult getSchoolClass(SchoolClassDetailQuery query);

    void assignUser(AssignUserToClassCommand command);
}
