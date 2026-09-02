package top.egon.cola.archetype.source.web.application.teaching.assemblers;

import top.egon.cola.archetype.source.web.application.teaching.result.GradeDetailResult;
import top.egon.cola.archetype.source.web.domain.teaching.entities.Grade;

public final class GradeAssembler {
    public GradeDetailResult toResult(Grade grade) {
        return new GradeDetailResult(grade.id(), grade.code().value(), grade.name(), grade.status().name());
    }
}
