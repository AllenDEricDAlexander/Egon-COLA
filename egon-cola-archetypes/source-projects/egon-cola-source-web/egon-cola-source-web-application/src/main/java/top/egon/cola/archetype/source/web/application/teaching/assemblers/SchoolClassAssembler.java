package top.egon.cola.archetype.source.web.application.teaching.assemblers;

import top.egon.cola.archetype.source.web.application.teaching.result.SchoolClassDetailResult;
import top.egon.cola.archetype.source.web.domain.teaching.entities.SchoolClass;
import top.egon.cola.archetype.source.web.domain.user.vos.UserId;

public final class SchoolClassAssembler {
    public SchoolClassDetailResult toResult(SchoolClass schoolClass) {
        return new SchoolClassDetailResult(schoolClass.id().value(), schoolClass.name(),
            schoolClass.gradeCode().value(), schoolClass.gradeName(), schoolClass.status().name(),
            schoolClass.userIds().stream().map(UserId::value).toList());
    }
}
