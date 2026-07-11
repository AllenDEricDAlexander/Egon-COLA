package ${package}.domain.service.teaching.impl;

import ${package}.domain.entities.teaching.Grade;
import ${package}.domain.enums.teaching.GradeStatus;
import ${package}.domain.service.teaching.GradeDomainService;
import ${package}.domain.validators.teaching.TeachingDomainValidator;
import ${package}.domain.vos.teaching.GradeCode;

public final class GradeDomainServiceImpl implements GradeDomainService {
    @Override
    public Grade create(String gradeId, String code, String name) {
        if (gradeId == null || !gradeId.startsWith("grade-")) {
            throw new IllegalArgumentException("new grade ids must start with grade-");
        }
        return new Grade(gradeId, GradeCode.create(code),
            TeachingDomainValidator.normalizeName(name, "grade name"), GradeStatus.ACTIVE);
    }
}
