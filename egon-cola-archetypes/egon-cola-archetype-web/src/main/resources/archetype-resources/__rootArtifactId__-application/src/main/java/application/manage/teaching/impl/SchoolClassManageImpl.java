package ${package}.application.manage.teaching.impl;

import ${package}.application.assemblers.teaching.SchoolClassAssembler;
import ${package}.application.command.teaching.CreateSchoolClassCommand;
import ${package}.application.command.teaching.AssignUserToClassCommand;
import ${package}.application.exceptions.OrganizationApplicationException;
import ${package}.application.exceptions.OrganizationFailureType;
import ${package}.application.manage.teaching.SchoolClassManage;
import ${package}.application.query.teaching.SchoolClassDetailQuery;
import ${package}.application.result.teaching.SchoolClassDetailResult;
import ${package}.application.validators.teaching.TeachingApplicationValidator;
import ${package}.domain.entities.teaching.Grade;
import ${package}.domain.entities.teaching.SchoolClass;
import ${package}.domain.aggregates.teaching.SchoolClassAggregate;
import ${package}.domain.repos.teaching.GradeRepository;
import ${package}.domain.repos.teaching.SchoolClassRepository;
import ${package}.domain.repos.user.UserRepository;
import ${package}.domain.service.teaching.SchoolClassDomainService;
import ${package}.domain.vos.teaching.GradeCode;
import ${package}.domain.vos.teaching.SchoolClassId;
import ${package}.domain.vos.user.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service("schoolClassManage")
public class SchoolClassManageImpl implements SchoolClassManage {
    private final SchoolClassRepository schoolClassRepository;
    private final GradeRepository gradeRepository;
    private final UserRepository userRepository;
    private final SchoolClassDomainService schoolClassDomainService;
    private final TeachingApplicationValidator validator;
    private final SchoolClassAssembler assembler = new SchoolClassAssembler();

    public SchoolClassManageImpl(
            SchoolClassRepository schoolClassRepository,
            GradeRepository gradeRepository,
            UserRepository userRepository,
            SchoolClassDomainService schoolClassDomainService,
            TeachingApplicationValidator validator) {
        this.schoolClassRepository = schoolClassRepository;
        this.gradeRepository = gradeRepository;
        this.userRepository = userRepository;
        this.schoolClassDomainService = schoolClassDomainService;
        this.validator = validator;
    }

    @Override
    @Transactional
    public SchoolClassDetailResult createSchoolClass(CreateSchoolClassCommand command) {
        validator.requireTeachingAdmin();
        Grade grade = gradeRepository.findByCode(GradeCode.create(command.gradeCode()))
            .orElseThrow(() -> notFound("grade not found"));
        if (schoolClassRepository.existsByGradeIdAndNameIgnoreCase(grade.id(), command.name().trim())) {
            throw conflict("school class name already exists in grade");
        }
        SchoolClass schoolClass = schoolClassDomainService.create(
            new SchoolClassId("class-" + UUID.randomUUID().toString().toLowerCase()), command.name(), grade);
        return assembler.toResult(schoolClassRepository.save(schoolClass));
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolClassDetailResult getSchoolClass(SchoolClassDetailQuery query) {
        return schoolClassRepository.findById(new SchoolClassId(query.schoolClassId()))
            .map(assembler::toResult)
            .orElseThrow(() -> notFound("school class not found"));
    }

    @Override
    @Transactional
    public void assignUser(AssignUserToClassCommand command) {
        validator.requireTeachingAdmin();
        SchoolClassId classId = new SchoolClassId(command.schoolClassId());
        UserId memberId = new UserId(command.userId());
        var user = userRepository.findById(memberId).orElseThrow(() -> notFound("user not found"));
        SchoolClass schoolClass = schoolClassRepository.findById(classId)
            .orElseThrow(() -> notFound("school class not found"));
        new SchoolClassAggregate(schoolClass).validateAssignment(user);
        if (schoolClassRepository.hasUser(classId, memberId)) {
            throw conflict("user already assigned to school class");
        }
        schoolClassRepository.addUser(classId, memberId);
    }

    private static OrganizationApplicationException conflict(String message) {
        return new OrganizationApplicationException(OrganizationFailureType.CONFLICT, "ORG_CONFLICT", message);
    }

    private static OrganizationApplicationException notFound(String message) {
        return new OrganizationApplicationException(OrganizationFailureType.NOT_FOUND, "ORG_NOT_FOUND", message);
    }
}
