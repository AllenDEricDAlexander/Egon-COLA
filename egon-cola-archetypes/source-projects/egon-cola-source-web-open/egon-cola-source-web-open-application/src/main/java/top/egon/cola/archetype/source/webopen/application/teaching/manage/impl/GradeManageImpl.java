package top.egon.cola.archetype.source.webopen.application.teaching.manage.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.archetype.source.webopen.application.support.IdempotentCommand;
import top.egon.cola.archetype.source.webopen.application.support.OrganizationTransactionHooks;
import top.egon.cola.archetype.source.webopen.application.teaching.manage.GradeManage;
import top.egon.cola.archetype.source.webopen.application.teaching.pojo.command.CreateGradeCommand;
import top.egon.cola.archetype.source.webopen.application.teaching.pojo.convertor.GradeConverter;
import top.egon.cola.archetype.source.webopen.application.teaching.pojo.query.GradeDetailQuery;
import top.egon.cola.archetype.source.webopen.application.teaching.pojo.result.GradeDetailResult;
import top.egon.cola.archetype.source.webopen.application.teaching.validators.TeachingApplicationValidator;
import top.egon.cola.archetype.source.webopen.common.enums.OrganizationFailureType;
import top.egon.cola.archetype.source.webopen.common.exception.OrganizationApplicationException;
import top.egon.cola.archetype.source.webopen.domain.service.CommandIdempotencyService;
import top.egon.cola.archetype.source.webopen.domain.service.OrganizationEventService;
import top.egon.cola.archetype.source.webopen.domain.teaching.entities.Grade;
import top.egon.cola.archetype.source.webopen.domain.teaching.events.GradeChangedEvent;
import top.egon.cola.archetype.source.webopen.domain.teaching.service.GradeDomainService;
import top.egon.cola.archetype.source.webopen.domain.teaching.vos.GradeCode;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;

import java.time.Instant;

/** Grade use cases; caching and transport stay inside the domain service implementations. */
@Service("gradeManage")
@Validated
@Slf4j
@RequiredArgsConstructor
public class GradeManageImpl implements GradeManage {

    @Qualifier("gradeDomainService")
    private final GradeDomainService gradeDomainService;
    @Qualifier("teachingApplicationValidator")
    private final TeachingApplicationValidator validator;
    @Qualifier("commandIdempotencyService")
    private final CommandIdempotencyService idempotency;
    @Qualifier("organizationEventService")
    private final OrganizationEventService eventService;
    @Qualifier("gradeConverterImpl")
    private final GradeConverter converter;

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
                    SnowflakeIdGenerator.nextLongId(), code.value(), command.name()));
            OrganizationTransactionHooks.afterCommit(() -> eventService.publish(
                    new GradeChangedEvent(
                            Long.toString(SnowflakeIdGenerator.nextLongId()),
                            grade.id(), Instant.now(), "CREATED")));
            return converter.toResult(grade);
        });
    }

    @Override
    public GradeDetailResult getGrade(GradeDetailQuery query) {
        Grade grade = gradeDomainService.findById(query.gradeId())
                .orElseThrow(() -> notFound("grade not found"));
        return converter.toResult(grade);
    }

    private static OrganizationApplicationException conflict(String message) {
        return new OrganizationApplicationException(
                OrganizationFailureType.CONFLICT, "ORG_CONFLICT", message);
    }

    private static OrganizationApplicationException notFound(String message) {
        return new OrganizationApplicationException(
                OrganizationFailureType.NOT_FOUND, "ORG_NOT_FOUND", message);
    }
}
