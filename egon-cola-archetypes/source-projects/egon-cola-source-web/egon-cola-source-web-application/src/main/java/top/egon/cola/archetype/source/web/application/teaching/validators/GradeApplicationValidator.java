package top.egon.cola.archetype.source.web.application.teaching.validators;

import top.egon.cola.archetype.source.web.domain.teaching.vos.GradeCode;

public final class GradeApplicationValidator {
    public GradeCode gradeCode(String raw) { return GradeCode.create(raw); }
}
