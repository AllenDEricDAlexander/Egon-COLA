package ${package}.domain.teaching.service.impl;

import ${package}.domain.teaching.entities.Grade;
import ${package}.domain.teaching.enums.GradeStatus;
import ${package}.domain.teaching.service.GradeDomainService;
import ${package}.domain.teaching.validators.TeachingDomainValidator;
import ${package}.domain.teaching.vos.GradeCode;

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
