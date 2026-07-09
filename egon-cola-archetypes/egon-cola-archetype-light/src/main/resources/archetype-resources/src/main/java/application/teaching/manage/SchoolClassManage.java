package ${package}.application.teaching.manage;

import ${package}.application.teaching.command.CreateSchoolClassCommand;
import ${package}.application.teaching.command.ScheduleCourseCommand;
import ${package}.application.teaching.query.GetSchoolClassQuery;
import ${package}.application.teaching.result.SchoolClassResult;

public interface SchoolClassManage {
    SchoolClassResult create(CreateSchoolClassCommand command);

    SchoolClassResult schedule(ScheduleCourseCommand command);

    SchoolClassResult get(GetSchoolClassQuery query);
}
