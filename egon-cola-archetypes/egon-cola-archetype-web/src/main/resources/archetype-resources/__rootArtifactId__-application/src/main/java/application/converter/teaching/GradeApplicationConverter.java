package ${package}.application.converter.teaching;

import ${package}.application.command.teaching.CreateGradeCommand;

public final class GradeApplicationConverter {
    public CreateGradeCommand toCommand(String requestId, String code, String name) {
        return new CreateGradeCommand(requestId, code, name);
    }
}
