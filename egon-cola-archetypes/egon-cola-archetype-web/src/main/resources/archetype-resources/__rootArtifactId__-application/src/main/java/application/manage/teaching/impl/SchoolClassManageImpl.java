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
import ${package}.domain.client.CommandIdempotencyPort;
import ${package}.domain.client.OrganizationEventPublisher;
import ${package}.domain.events.teaching.SchoolClassChangedEvent;
import ${package}.domain.events.teaching.SchoolClassMembershipChangedEvent;
import ${package}.domain.client.teaching.SchoolClassCachePort;
import ${package}.application.support.IdempotentCommand;
import ${package}.application.support.OrganizationTransactionHooks;
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
import java.time.Instant;

@Service("schoolClassManage")
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

    public SchoolClassManageImpl(
            SchoolClassRepository schoolClassRepository,
            GradeRepository gradeRepository,
            UserRepository userRepository,
            SchoolClassDomainService schoolClassDomainService,
            TeachingApplicationValidator validator,
            SchoolClassCachePort schoolClassCache,
            CommandIdempotencyPort idempotency,
            OrganizationEventPublisher eventPublisher) {
        this.schoolClassRepository = schoolClassRepository;
        this.gradeRepository = gradeRepository;
        this.userRepository = userRepository;
        this.schoolClassDomainService = schoolClassDomainService;
        this.validator = validator;
        this.schoolClassCache = schoolClassCache;
        this.idempotency = idempotency;
        this.eventPublisher = eventPublisher;
    }

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
            new SchoolClassAggregate(schoolClass).validateAssignment(user);
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
