package ${package}.application.teaching.manage.impl;

import ${package}.application.teaching.assemblers.SchoolClassAssembler;
import ${package}.application.teaching.command.CreateSchoolClassCommand;
import ${package}.application.teaching.command.AssignUserToClassCommand;
import ${package}.application.exceptions.OrganizationApplicationException;
import ${package}.application.exceptions.OrganizationFailureType;
import ${package}.application.teaching.manage.SchoolClassManage;
import ${package}.application.teaching.query.SchoolClassDetailQuery;
import ${package}.application.teaching.result.SchoolClassDetailResult;
import ${package}.application.teaching.validators.TeachingApplicationValidator;
import ${package}.domain.teaching.entities.Grade;
import ${package}.domain.client.CommandIdempotencyPort;
import ${package}.domain.client.OrganizationEventPublisher;
import ${package}.domain.teaching.events.SchoolClassChangedEvent;
import ${package}.domain.teaching.events.SchoolClassMembershipChangedEvent;
import ${package}.domain.teaching.client.SchoolClassCachePort;
import ${package}.application.support.IdempotentCommand;
import ${package}.application.support.OrganizationTransactionHooks;
import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.teaching.aggregates.SchoolClassAggregate;
import ${package}.domain.exceptions.OrganizationDomainException;
import ${package}.domain.teaching.service.SchoolClassDomainService;
import ${package}.domain.user.service.UserDomainService;
import ${package}.domain.teaching.vos.GradeCode;
import ${package}.domain.teaching.vos.SchoolClassId;
import ${package}.domain.user.vos.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;

import java.time.Instant;

@Service("schoolClassManage")
@RequiredArgsConstructor
public class SchoolClassManageImpl implements SchoolClassManage {
    private final SchoolClassDomainService<?> schoolClassDomainService;
    private final UserDomainService<?> userDomainService;
    private final TeachingApplicationValidator validator;
    private final SchoolClassCachePort schoolClassCache;
    private final CommandIdempotencyPort idempotency;
    private final OrganizationEventPublisher eventPublisher;
    private final LongIdGenerator idGenerator;
    private final SchoolClassAssembler assembler = new SchoolClassAssembler();

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
                new SchoolClassId(idGenerator.nextLongId()), command.name(), grade));
            OrganizationTransactionHooks.afterCommit(() -> {
                schoolClassCache.evict(schoolClass.gradeId(), schoolClass.id());
                eventPublisher.publish(new SchoolClassChangedEvent(Long.toString(idGenerator.nextLongId()),
                    schoolClass.id().value(), Instant.now(), schoolClass.gradeId(), "CREATED"));
            });
            return assembler.toResult(schoolClass);
        });
    }

    @Override
    public SchoolClassDetailResult getSchoolClass(SchoolClassDetailQuery query) {
        SchoolClassId id = new SchoolClassId(query.schoolClassId());
        SchoolClass schoolClass = schoolClassCache.findById(query.gradeId(), id).orElseGet(() -> {
            SchoolClass loaded = schoolClassDomainService.findByGradeIdAndId(query.gradeId(), id)
                .orElseThrow(() -> notFound("school class not found"));
            schoolClassCache.put(loaded);
            return loaded;
        });
        return assembler.toResult(schoolClass);
    }

    @Override
    @Transactional
    public void assignUser(AssignUserToClassCommand command) {
        IdempotentCommand.execute(idempotency, "assign-user-to-school-class", command.requestId(), () -> {
            validator.requireTeachingAdmin();
            SchoolClassId classId = new SchoolClassId(command.schoolClassId());
            UserId memberId = new UserId(command.userId());
            var user = userDomainService.findById(memberId).orElseThrow(() -> notFound("user not found"));
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
            OrganizationTransactionHooks.afterCommit(() -> {
                schoolClassCache.evict(command.gradeId(), classId);
                eventPublisher.publish(new SchoolClassMembershipChangedEvent(Long.toString(idGenerator.nextLongId()),
                    classId.value(), Instant.now(), memberId.value(), "ASSIGNED"));
            });
        });
    }

    private static OrganizationApplicationException conflict(String message) {
        return new OrganizationApplicationException(OrganizationFailureType.CONFLICT, "ORG_CONFLICT", message);
    }

    private static OrganizationApplicationException notFound(String message) {
        return new OrganizationApplicationException(OrganizationFailureType.NOT_FOUND, "ORG_NOT_FOUND", message);
    }
}
