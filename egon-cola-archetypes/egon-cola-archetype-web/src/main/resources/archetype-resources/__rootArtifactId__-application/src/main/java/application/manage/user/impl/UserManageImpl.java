package ${package}.application.manage.user.impl;

import ${package}.application.assemblers.user.UserAssembler;
import ${package}.application.command.user.CreateUserCommand;
import ${package}.application.exceptions.OrganizationApplicationException;
import ${package}.application.exceptions.OrganizationFailureType;
import ${package}.application.manage.user.UserManage;
import ${package}.application.query.user.UserDetailQuery;
import ${package}.application.result.user.UserDetailResult;
import ${package}.application.validators.user.UserApplicationValidator;
import ${package}.domain.entities.user.User;
import ${package}.domain.client.CommandIdempotencyPort;
import ${package}.domain.client.OrganizationEventPublisher;
import ${package}.domain.events.user.UserChangedEvent;
import ${package}.domain.client.user.UserCachePort;
import ${package}.application.support.IdempotentCommand;
import ${package}.application.support.OrganizationTransactionHooks;
import ${package}.domain.repos.user.UserRepository;
import ${package}.domain.service.user.UserDomainService;
import ${package}.domain.vos.user.UserId;
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
