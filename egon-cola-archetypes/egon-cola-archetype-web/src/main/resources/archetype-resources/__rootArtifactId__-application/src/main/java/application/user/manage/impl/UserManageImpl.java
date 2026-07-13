package ${package}.application.user.manage.impl;

import ${package}.application.user.assemblers.UserAssembler;
import ${package}.application.user.command.CreateUserCommand;
import ${package}.application.exceptions.OrganizationApplicationException;
import ${package}.application.exceptions.OrganizationFailureType;
import ${package}.application.user.manage.UserManage;
import ${package}.application.user.query.UserDetailQuery;
import ${package}.application.user.result.UserDetailResult;
import ${package}.application.user.validators.UserApplicationValidator;
import ${package}.domain.user.entities.User;
import ${package}.domain.client.CommandIdempotencyPort;
import ${package}.domain.client.OrganizationEventPublisher;
import ${package}.domain.user.events.UserChangedEvent;
import ${package}.domain.user.client.UserCachePort;
import ${package}.application.support.IdempotentCommand;
import ${package}.application.support.OrganizationTransactionHooks;
import ${package}.domain.user.repos.UserRepository;
import ${package}.domain.user.service.UserDomainService;
import ${package}.domain.user.vos.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.time.Instant;

@Service("userManage")
public class UserManageImpl implements UserManage {

    private final UserRepository userRepository;
    private final UserDomainService userDomainService;
    private final UserApplicationValidator validator;
    private final UserAssembler assembler;
    private final UserCachePort userCache;
    private final CommandIdempotencyPort idempotency;
    private final OrganizationEventPublisher eventPublisher;

    public UserManageImpl(
            UserRepository userRepository,
            UserDomainService userDomainService,
            UserApplicationValidator validator,
            UserAssembler assembler,
            UserCachePort userCache,
            CommandIdempotencyPort idempotency,
            OrganizationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.userDomainService = userDomainService;
        this.validator = validator;
        this.assembler = assembler;
        this.userCache = userCache;
        this.idempotency = idempotency;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public UserDetailResult createUser(CreateUserCommand command) {
        return IdempotentCommand.execute(idempotency, "create-user", command.requestId(), () -> {
            validator.requireOrganizationAdmin();
            String normalizedEmail = validator.normalizedEmail(command.email());
            if (userRepository.existsByEmail(normalizedEmail)) {
                throw new OrganizationApplicationException(
                    OrganizationFailureType.CONFLICT, "ORG_CONFLICT", "user email already exists");
            }
            User user = userRepository.save(userDomainService.create(
                new UserId("user-" + UUID.randomUUID().toString().toLowerCase()), command.name(), normalizedEmail));
            OrganizationTransactionHooks.afterCommit(() -> {
                userCache.evict(user.id());
                eventPublisher.publish(new UserChangedEvent(
                    UUID.randomUUID().toString(), user.id().value(), Instant.now(), "CREATED"));
            });
            return assembler.toResult(user);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailResult getUser(UserDetailQuery query) {
        UserId userId = new UserId(query.userId());
        User user = userCache.findById(userId).orElseGet(() -> {
            User loaded = userRepository.findById(userId)
            .orElseThrow(() -> new OrganizationApplicationException(
                OrganizationFailureType.NOT_FOUND, "ORG_NOT_FOUND", "user not found"));
            userCache.put(loaded);
            return loaded;
        });
        return assembler.toResult(user);
    }
}
