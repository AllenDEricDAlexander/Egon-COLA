package top.egon.cola.archetype.source.web.application.teaching.converter;

import top.egon.cola.archetype.source.web.application.teaching.command.CreateSchoolClassCommand;

public final class SchoolClassApplicationConverter {
    public CreateSchoolClassCommand toCommand(String requestId, String name, String gradeCode) {
        return new CreateSchoolClassCommand(requestId, name, gradeCode);
    }
}
