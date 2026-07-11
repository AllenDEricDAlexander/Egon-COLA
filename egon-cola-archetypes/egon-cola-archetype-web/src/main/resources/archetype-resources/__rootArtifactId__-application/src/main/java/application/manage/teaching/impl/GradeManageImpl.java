package ${package}.application.manage.teaching.impl;

import ${package}.application.assemblers.teaching.GradeAssembler;
import ${package}.application.command.teaching.CreateGradeCommand;
import ${package}.application.exceptions.OrganizationApplicationException;
import ${package}.application.exceptions.OrganizationFailureType;
import ${package}.application.manage.teaching.GradeManage;
import ${package}.application.query.teaching.GradeDetailQuery;
import ${package}.application.result.teaching.GradeDetailResult;
import ${package}.application.validators.teaching.TeachingApplicationValidator;
import ${package}.domain.entities.teaching.Grade;
import ${package}.domain.repos.teaching.GradeRepository;
import ${package}.domain.service.teaching.GradeDomainService;
import ${package}.domain.vos.teaching.GradeCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service("gradeManage")
public class GradeManageImpl implements GradeManage {
    private final GradeRepository gradeRepository;
    private final GradeDomainService gradeDomainService;
    private final TeachingApplicationValidator validator;
    private final GradeAssembler assembler = new GradeAssembler();

    public GradeManageImpl(
            GradeRepository gradeRepository,
            GradeDomainService gradeDomainService,
            TeachingApplicationValidator validator) {
        this.gradeRepository = gradeRepository;
        this.gradeDomainService = gradeDomainService;
        this.validator = validator;
    }

    @Override
    @Transactional
    public GradeDetailResult createGrade(CreateGradeCommand command) {
        validator.requireTeachingAdmin();
        GradeCode code = GradeCode.create(command.code());
        if (gradeRepository.existsByCode(code)) {
            throw conflict("grade code already exists");
        }
        Grade grade = gradeDomainService.create(
            "grade-" + UUID.randomUUID().toString().toLowerCase(), code.value(), command.name());
        return assembler.toResult(gradeRepository.save(grade));
    }

    @Override
    @Transactional(readOnly = true)
    public GradeDetailResult getGrade(GradeDetailQuery query) {
        return gradeRepository.findById(query.gradeId()).map(assembler::toResult)
            .orElseThrow(() -> notFound("grade not found"));
    }

    private static OrganizationApplicationException conflict(String message) {
        return new OrganizationApplicationException(OrganizationFailureType.CONFLICT, "ORG_CONFLICT", message);
    }

    private static OrganizationApplicationException notFound(String message) {
        return new OrganizationApplicationException(OrganizationFailureType.NOT_FOUND, "ORG_NOT_FOUND", message);
    }
}
