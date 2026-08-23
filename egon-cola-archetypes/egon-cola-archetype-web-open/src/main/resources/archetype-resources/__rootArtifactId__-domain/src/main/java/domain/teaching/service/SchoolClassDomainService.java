package ${package}.domain.teaching.service;

import ${package}.domain.teaching.entities.Grade;
import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.teaching.enums.GradeStatus;
import ${package}.domain.teaching.enums.SchoolClassStatus;
import ${package}.domain.exceptions.OrganizationDomainErrorCode;
import ${package}.domain.exceptions.OrganizationDomainException;
import ${package}.domain.teaching.validators.TeachingDomainValidator;
import ${package}.domain.teaching.vos.GradeCode;
import ${package}.domain.teaching.vos.SchoolClassId;
import ${package}.domain.user.vos.UserId;

import java.util.List;

public class SchoolClassDomainService {
    public SchoolClass create(SchoolClassId schoolClassId, String name, Grade grade) {
        if (grade.status() == GradeStatus.ARCHIVED) {
            throw new OrganizationDomainException(
                OrganizationDomainErrorCode.DOMAIN_REJECTED, "archived grade cannot receive school classes");
        }
        return new SchoolClass(schoolClassId, TeachingDomainValidator.normalizeName(name, "school class name"),
            grade.id(), grade.code(), grade.name(), SchoolClassStatus.ACTIVE, List.of());
    }

    public SchoolClass assignUser(SchoolClass schoolClass, String userId) {
        UserId id = new UserId(userId);
        if (schoolClass.hasUser(id)) {
            throw new OrganizationDomainException(
                OrganizationDomainErrorCode.CONFLICT, "user already assigned to school class");
        }
        schoolClass.assignUser(id);
        return schoolClass;
    }
}
