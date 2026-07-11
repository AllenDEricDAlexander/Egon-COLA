package ${package}.application.validators.teaching;

import ${package}.domain.vos.teaching.GradeCode;

public final class GradeApplicationValidator {
    public GradeCode gradeCode(String raw) { return GradeCode.create(raw); }
}
