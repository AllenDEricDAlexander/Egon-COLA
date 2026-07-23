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
import ${package}.domain.teaching.repos.GradeRepository;
import ${package}.domain.teaching.repos.SchoolClassRepository;
import ${package}.domain.user.repos.UserRepository;
import ${package}.domain.teaching.service.SchoolClassDomainService;
import ${package}.domain.teaching.vos.GradeCode;
import ${package}.domain.teaching.vos.SchoolClassId;
import ${package}.domain.user.vos.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.time.Instant;

@Service("schoolClassManage")
@RequiredArgsConstructor
public class SchoolClassManageImpl implements SchoolClassManage {
    private final SchoolClassRepository schoolClassRepository;
    private final GradeRepository gradeRepository;
    private final UserRepository userRepository;
    private final SchoolClassDomainService schoolClassDomainService;
    private final TeachingApplicationValidator validator;
    private final SchoolClassCachePort schoolClassCache;
    private final CommandIdempotencyPort idempotency;
    private final OrganizationEventPublisher eventPublisher;
    private final SchoolClassAssembler assembler = new SchoolClassAssembler();

    @Override
    @Transactional
    public SchoolClassDetailResult createSchoolClass(CreateSchoolClassCommand command) {
        return IdempotentCommand.execute(idempotency, "create-school-class", command.requestId(), () -> {
            validator.requireTeachingAdmin();
            Grade grade = gradeRepository.findByCode(GradeCode.create(command.gradeCode()))
                .orElseThrow(() -> notFound("grade not found"));
            if (schoolClassRepository.existsByGradeIdAndNameIgnoreCase(grade.id(), command.name().trim())) {
                throw conflict("school class name already exists in grade");
            }
            SchoolClass schoolClass = schoolClassRepository.save(schoolClassDomainService.create(
                new SchoolClassId("class-" + UUID.randomUUID().toString().toLowerCase()), command.name(), grade));
            OrganizationTransactionHooks.afterCommit(() -> {
                schoolClassCache.evict(schoolClass.id());
                eventPublisher.publish(new SchoolClassChangedEvent(UUID.randomUUID().toString(),
                    schoolClass.id().value(), Instant.now(), schoolClass.gradeId(), "CREATED"));
            });
            return assembler.toResult(schoolClass);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolClassDetailResult getSchoolClass(SchoolClassDetailQuery query) {
        SchoolClassId id = new SchoolClassId(query.schoolClassId());
        SchoolClass schoolClass = schoolClassCache.findById(id).orElseGet(() -> {
            SchoolClass loaded = schoolClassRepository.findById(id)
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
            var user = userRepository.findById(memberId).orElseThrow(() -> notFound("user not found"));
            SchoolClass schoolClass = schoolClassRepository.findById(classId)
                .orElseThrow(() -> notFound("school class not found"));
            try {
                new SchoolClassAggregate(schoolClass).validateAssignment(user);
            } catch (OrganizationDomainException failure) {
                throw new OrganizationApplicationException(
                        OrganizationFailureType.DOMAIN_REJECTED,
                        "ORG_DOMAIN_REJECTED",
                        failure.getMessage());
            }
            if (schoolClassRepository.hasUser(classId, memberId)) {
                throw conflict("user already assigned to school class");
            }
            schoolClassRepository.addUser(classId, memberId);
            OrganizationTransactionHooks.afterCommit(() -> {
                schoolClassCache.evict(classId);
                eventPublisher.publish(new SchoolClassMembershipChangedEvent(UUID.randomUUID().toString(),
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
