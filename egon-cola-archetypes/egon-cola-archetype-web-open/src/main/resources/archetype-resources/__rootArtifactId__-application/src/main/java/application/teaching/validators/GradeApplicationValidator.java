package ${package}.application.teaching.validators;

import ${package}.domain.teaching.vos.GradeCode;

public final class GradeApplicationValidator {
    public GradeCode gradeCode(String raw) { return GradeCode.create(raw); }
}
