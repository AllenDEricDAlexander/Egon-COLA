package ${package}.application.teaching.manage.impl;

import ${package}.application.teaching.assemblers.GradeAssembler;
import ${package}.application.teaching.command.CreateGradeCommand;
import ${package}.application.exceptions.OrganizationApplicationException;
import ${package}.application.exceptions.OrganizationFailureType;
import ${package}.application.teaching.manage.GradeManage;
import ${package}.application.teaching.query.GradeDetailQuery;
import ${package}.application.teaching.result.GradeDetailResult;
import ${package}.application.teaching.validators.TeachingApplicationValidator;
import ${package}.domain.teaching.entities.Grade;
import ${package}.domain.client.CommandIdempotencyPort;
import ${package}.domain.client.OrganizationEventPublisher;
import ${package}.domain.teaching.events.GradeChangedEvent;
import ${package}.domain.teaching.client.GradeCachePort;
import ${package}.application.support.IdempotentCommand;
import ${package}.application.support.OrganizationTransactionHooks;
import ${package}.domain.teaching.repos.GradeRepository;
import ${package}.domain.teaching.service.GradeDomainService;
import ${package}.domain.teaching.vos.GradeCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.time.Instant;

@Service("gradeManage")
public class GradeManageImpl implements GradeManage {
    private final GradeRepository gradeRepository;
    private final GradeDomainService gradeDomainService;
    private final TeachingApplicationValidator validator;
    private final GradeCachePort gradeCache;
    private final CommandIdempotencyPort idempotency;
    private final OrganizationEventPublisher eventPublisher;
    private final GradeAssembler assembler = new GradeAssembler();

    public GradeManageImpl(
            GradeRepository gradeRepository,
            GradeDomainService gradeDomainService,
            TeachingApplicationValidator validator,
            GradeCachePort gradeCache,
            CommandIdempotencyPort idempotency,
            OrganizationEventPublisher eventPublisher) {
        this.gradeRepository = gradeRepository;
        this.gradeDomainService = gradeDomainService;
        this.validator = validator;
        this.gradeCache = gradeCache;
        this.idempotency = idempotency;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public GradeDetailResult createGrade(CreateGradeCommand command) {
        return IdempotentCommand.execute(idempotency, "create-grade", command.requestId(), () -> {
            validator.requireTeachingAdmin();
            GradeCode code = GradeCode.create(command.code());
            if (gradeRepository.existsByCode(code)) {
                throw conflict("grade code already exists");
            }
            Grade grade = gradeRepository.save(gradeDomainService.create(
                "grade-" + UUID.randomUUID().toString().toLowerCase(), code.value(), command.name()));
            OrganizationTransactionHooks.afterCommit(() -> {
                gradeCache.evict(grade.id());
                eventPublisher.publish(new GradeChangedEvent(UUID.randomUUID().toString(),
                    grade.id(), Instant.now(), "CREATED"));
            });
            return assembler.toResult(grade);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public GradeDetailResult getGrade(GradeDetailQuery query) {
        Grade grade = gradeCache.findById(query.gradeId()).orElseGet(() -> {
            Grade loaded = gradeRepository.findById(query.gradeId())
                .orElseThrow(() -> notFound("grade not found"));
            gradeCache.put(loaded);
            return loaded;
        });
        return assembler.toResult(grade);
    }

    private static OrganizationApplicationException conflict(String message) {
        return new OrganizationApplicationException(OrganizationFailureType.CONFLICT, "ORG_CONFLICT", message);
    }

    private static OrganizationApplicationException notFound(String message) {
        return new OrganizationApplicationException(OrganizationFailureType.NOT_FOUND, "ORG_NOT_FOUND", message);
    }
}
