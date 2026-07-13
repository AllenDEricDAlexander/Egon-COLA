package ${package}.application.teaching.converter;

import ${package}.application.teaching.command.CreateSchoolClassCommand;

public final class SchoolClassApplicationConverter {
    public CreateSchoolClassCommand toCommand(String requestId, String name, String gradeCode) {
        return new CreateSchoolClassCommand(requestId, name, gradeCode);
    }
}
