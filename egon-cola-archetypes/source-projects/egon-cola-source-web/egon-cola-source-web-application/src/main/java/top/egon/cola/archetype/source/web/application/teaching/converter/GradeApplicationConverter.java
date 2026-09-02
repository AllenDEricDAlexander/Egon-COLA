package top.egon.cola.archetype.source.web.application.teaching.converter;

import top.egon.cola.archetype.source.web.application.teaching.command.CreateGradeCommand;

public final class GradeApplicationConverter {
    public CreateGradeCommand toCommand(String requestId, String code, String name) {
        return new CreateGradeCommand(requestId, code, name);
    }
}
