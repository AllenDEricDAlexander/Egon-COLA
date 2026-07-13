package ${package}.application.teaching.converter;

import ${package}.application.teaching.command.CreateGradeCommand;

public final class GradeApplicationConverter {
    public CreateGradeCommand toCommand(String requestId, String code, String name) {
        return new CreateGradeCommand(requestId, code, name);
    }
}
