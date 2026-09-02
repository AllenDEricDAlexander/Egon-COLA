package top.egon.cola.archetype.source.webopen.application.teaching.manage.impl;

import top.egon.cola.archetype.source.webopen.application.teaching.assemblers.GradeAssembler;
import top.egon.cola.archetype.source.webopen.application.teaching.command.CreateGradeCommand;
import top.egon.cola.archetype.source.webopen.application.exceptions.OrganizationApplicationException;
import top.egon.cola.archetype.source.webopen.application.exceptions.OrganizationFailureType;
import top.egon.cola.archetype.source.webopen.application.teaching.manage.GradeManage;
import top.egon.cola.archetype.source.webopen.application.teaching.query.GradeDetailQuery;
import top.egon.cola.archetype.source.webopen.application.teaching.result.GradeDetailResult;
import top.egon.cola.archetype.source.webopen.application.teaching.validators.TeachingApplicationValidator;
import top.egon.cola.archetype.source.webopen.domain.teaching.entities.Grade;
import top.egon.cola.archetype.source.webopen.domain.client.CommandIdempotencyPort;
import top.egon.cola.archetype.source.webopen.domain.client.OrganizationEventPublisher;
import top.egon.cola.archetype.source.webopen.domain.teaching.events.GradeChangedEvent;
import top.egon.cola.archetype.source.webopen.domain.teaching.client.GradeCachePort;
import top.egon.cola.archetype.source.webopen.application.support.IdempotentCommand;
import top.egon.cola.archetype.source.webopen.application.support.OrganizationTransactionHooks;
import top.egon.cola.archetype.source.webopen.domain.teaching.service.GradeDomainService;
import top.egon.cola.archetype.source.webopen.domain.teaching.vos.GradeCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;

import java.time.Instant;

@Service("gradeManage")
@RequiredArgsConstructor
public class GradeManageImpl implements GradeManage {
    private final GradeDomainService<?> gradeDomainService;
    private final TeachingApplicationValidator validator;
    private final GradeCachePort gradeCache;
    private final CommandIdempotencyPort idempotency;
    private final OrganizationEventPublisher eventPublisher;
    private final LongIdGenerator idGenerator;
    private final GradeAssembler assembler = new GradeAssembler();

    @Override
    @Transactional
    public GradeDetailResult createGrade(CreateGradeCommand command) {
        return IdempotentCommand.execute(idempotency, "create-grade", command.requestId(), () -> {
            validator.requireTeachingAdmin();
            GradeCode code = GradeCode.create(command.code());
            if (gradeDomainService.existsByCode(code)) {
                throw conflict("grade code already exists");
            }
            Grade grade = gradeDomainService.save(gradeDomainService.create(
                idGenerator.nextLongId(), code.value(), command.name()));
            OrganizationTransactionHooks.afterCommit(() -> {
                gradeCache.evict(grade.id());
                eventPublisher.publish(new GradeChangedEvent(Long.toString(idGenerator.nextLongId()),
                    grade.id(), Instant.now(), "CREATED"));
            });
            return assembler.toResult(grade);
        });
    }

    @Override
    public GradeDetailResult getGrade(GradeDetailQuery query) {
        Grade grade = gradeCache.findById(query.gradeId()).orElseGet(() -> {
            Grade loaded = gradeDomainService.findById(query.gradeId())
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
