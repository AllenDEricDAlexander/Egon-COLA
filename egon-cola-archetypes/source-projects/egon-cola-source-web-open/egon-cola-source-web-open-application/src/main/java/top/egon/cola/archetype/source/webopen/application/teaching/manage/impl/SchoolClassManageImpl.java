package top.egon.cola.archetype.source.webopen.application.teaching.manage.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.archetype.source.webopen.application.support.IdempotentCommand;
import top.egon.cola.archetype.source.webopen.application.support.OrganizationTransactionHooks;
import top.egon.cola.archetype.source.webopen.application.teaching.manage.SchoolClassManage;
import top.egon.cola.archetype.source.webopen.application.teaching.pojo.command.AssignUserToClassCommand;
import top.egon.cola.archetype.source.webopen.application.teaching.pojo.command.CreateSchoolClassCommand;
import top.egon.cola.archetype.source.webopen.application.teaching.pojo.convertor.SchoolClassConverter;
import top.egon.cola.archetype.source.webopen.application.teaching.pojo.query.SchoolClassDetailQuery;
import top.egon.cola.archetype.source.webopen.application.teaching.pojo.result.SchoolClassDetailResult;
import top.egon.cola.archetype.source.webopen.application.teaching.validators.TeachingApplicationValidator;
import top.egon.cola.archetype.source.webopen.common.enums.OrganizationFailureType;
import top.egon.cola.archetype.source.webopen.common.exception.OrganizationApplicationException;
import top.egon.cola.archetype.source.webopen.common.exception.OrganizationDomainException;
import top.egon.cola.archetype.source.webopen.domain.service.CommandIdempotencyService;
import top.egon.cola.archetype.source.webopen.domain.service.OrganizationEventService;
import top.egon.cola.archetype.source.webopen.domain.teaching.aggregates.SchoolClassAggregate;
import top.egon.cola.archetype.source.webopen.domain.teaching.entities.Grade;
import top.egon.cola.archetype.source.webopen.domain.teaching.entities.SchoolClass;
import top.egon.cola.archetype.source.webopen.domain.teaching.events.SchoolClassChangedEvent;
import top.egon.cola.archetype.source.webopen.domain.teaching.events.SchoolClassMembershipChangedEvent;
import top.egon.cola.archetype.source.webopen.domain.teaching.service.SchoolClassDomainService;
import top.egon.cola.archetype.source.webopen.domain.teaching.vos.GradeCode;
import top.egon.cola.archetype.source.webopen.domain.teaching.vos.SchoolClassId;
import top.egon.cola.archetype.source.webopen.domain.user.entities.User;
import top.egon.cola.archetype.source.webopen.domain.user.service.UserDomainService;
import top.egon.cola.archetype.source.webopen.domain.user.vos.UserId;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;

import java.time.Instant;

/** School class and membership use cases; the compound lookup stays inside the domain service. */
@Service("schoolClassManage")
@Validated
@Slf4j
@RequiredArgsConstructor
public class SchoolClassManageImpl implements SchoolClassManage {

    @Qualifier("schoolClassDomainService")
    private final SchoolClassDomainService schoolClassDomainService;
    @Qualifier("userDomainService")
    private final UserDomainService userDomainService;
    @Qualifier("teachingApplicationValidator")
    private final TeachingApplicationValidator validator;
    @Qualifier("commandIdempotencyService")
    private final CommandIdempotencyService idempotency;
    @Qualifier("organizationEventService")
    private final OrganizationEventService eventService;
    @Qualifier("schoolClassConverterImpl")
    private final SchoolClassConverter converter;

    @Override
    @Transactional
    public SchoolClassDetailResult createSchoolClass(CreateSchoolClassCommand command) {
        return IdempotentCommand.execute(idempotency, "create-school-class", command.requestId(), () -> {
            validator.requireTeachingAdmin();
            Grade grade = schoolClassDomainService.findGradeByCode(GradeCode.create(command.gradeCode()))
                    .orElseThrow(() -> notFound("grade not found"));
            if (schoolClassDomainService.existsByGradeIdAndNameIgnoreCase(grade.id(), command.name().trim())) {
                throw conflict("school class name already exists in grade");
            }
            SchoolClass schoolClass = schoolClassDomainService.save(schoolClassDomainService.create(
                    new SchoolClassId(SnowflakeIdGenerator.nextLongId()), command.name(), grade));
            OrganizationTransactionHooks.afterCommit(() -> eventService.publish(
                    new SchoolClassChangedEvent(
                            Long.toString(SnowflakeIdGenerator.nextLongId()),
                            schoolClass.id().value(), Instant.now(), schoolClass.gradeId(), "CREATED")));
            return converter.toResult(schoolClass);
        });
    }

    @Override
    public SchoolClassDetailResult getSchoolClass(SchoolClassDetailQuery query) {
        SchoolClass schoolClass = schoolClassDomainService
                .findByGradeIdAndId(query.gradeId(), new SchoolClassId(query.schoolClassId()))
                .orElseThrow(() -> notFound("school class not found"));
        return converter.toResult(schoolClass);
    }

    @Override
    @Transactional
    public void assignUser(AssignUserToClassCommand command) {
        IdempotentCommand.execute(idempotency, "assign-user-to-school-class", command.requestId(), () -> {
            validator.requireTeachingAdmin();
            SchoolClassId classId = new SchoolClassId(command.schoolClassId());
            UserId memberId = new UserId(command.userId());
            User user = userDomainService.findById(memberId).orElseThrow(() -> notFound("user not found"));
            SchoolClass schoolClass = schoolClassDomainService.findByGradeIdAndId(command.gradeId(), classId)
                    .orElseThrow(() -> notFound("school class not found"));
            try {
                new SchoolClassAggregate(schoolClass).validateAssignment(user);
            } catch (OrganizationDomainException failure) {
                throw new OrganizationApplicationException(
                        OrganizationFailureType.DOMAIN_REJECTED,
                        "ORG_DOMAIN_REJECTED",
                        failure.getMessage());
            }
            if (schoolClassDomainService.hasUser(command.gradeId(), classId, memberId)) {
                throw conflict("user already assigned to school class");
            }
            schoolClassDomainService.addUser(command.gradeId(), classId, memberId);
            OrganizationTransactionHooks.afterCommit(() -> eventService.publish(
                    new SchoolClassMembershipChangedEvent(
                            Long.toString(SnowflakeIdGenerator.nextLongId()),
                            classId.value(), Instant.now(), memberId.value(), "ASSIGNED")));
        });
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
