package ${package}.application.converter.teaching;

import ${package}.application.command.teaching.CreateSchoolClassCommand;

public final class SchoolClassApplicationConverter {
    public CreateSchoolClassCommand toCommand(String requestId, String name, String gradeCode) {
        return new CreateSchoolClassCommand(requestId, name, gradeCode);
    }
}
