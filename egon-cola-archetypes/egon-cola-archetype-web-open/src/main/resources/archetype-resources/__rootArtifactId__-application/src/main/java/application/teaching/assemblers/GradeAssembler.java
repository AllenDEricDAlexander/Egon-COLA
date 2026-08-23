package ${package}.application.teaching.assemblers;

import ${package}.application.teaching.result.GradeDetailResult;
import ${package}.domain.teaching.entities.Grade;

public final class GradeAssembler {
    public GradeDetailResult toResult(Grade grade) {
        return new GradeDetailResult(grade.id(), grade.code().value(), grade.name(), grade.status().name());
    }
}
