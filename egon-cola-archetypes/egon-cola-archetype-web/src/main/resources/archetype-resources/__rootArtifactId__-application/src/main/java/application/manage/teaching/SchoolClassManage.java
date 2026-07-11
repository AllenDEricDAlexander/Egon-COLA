package ${package}.application.manage.teaching;

import ${package}.application.command.teaching.CreateSchoolClassCommand;
import ${package}.application.command.teaching.AssignUserToClassCommand;
import ${package}.application.query.teaching.SchoolClassDetailQuery;
import ${package}.application.result.teaching.SchoolClassDetailResult;

public interface SchoolClassManage {
    SchoolClassDetailResult createSchoolClass(CreateSchoolClassCommand command);
    SchoolClassDetailResult getSchoolClass(SchoolClassDetailQuery query);

    void assignUser(AssignUserToClassCommand command);
}
