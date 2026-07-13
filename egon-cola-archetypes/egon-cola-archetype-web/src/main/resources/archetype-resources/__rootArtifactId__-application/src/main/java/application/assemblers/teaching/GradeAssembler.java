package ${package}.application.assemblers.teaching;

import ${package}.application.result.teaching.GradeDetailResult;
import ${package}.domain.teaching.entities.Grade;

public final class GradeAssembler {
    public GradeDetailResult toResult(Grade grade) {
        return new GradeDetailResult(grade.id(), grade.code().value(), grade.name(), grade.status().name());
    }
}
