package ${package}.application.assemblers.teaching;

import ${package}.application.result.teaching.SchoolClassDetailResult;
import ${package}.domain.entities.teaching.SchoolClass;
import ${package}.domain.vos.user.UserId;

public final class SchoolClassAssembler {
    public SchoolClassDetailResult toResult(SchoolClass schoolClass) {
        return new SchoolClassDetailResult(schoolClass.id().value(), schoolClass.name(),
            schoolClass.gradeCode().value(), schoolClass.gradeName(), schoolClass.status().name(),
            schoolClass.userIds().stream().map(UserId::value).toList());
    }
}
