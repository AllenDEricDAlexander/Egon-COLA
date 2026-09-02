package top.egon.cola.archetype.source.webopen.application.user.manage.impl;

import top.egon.cola.archetype.source.webopen.application.user.assemblers.UserAssembler;
import top.egon.cola.archetype.source.webopen.application.user.command.CreateUserCommand;
import top.egon.cola.archetype.source.webopen.application.exceptions.OrganizationApplicationException;
import top.egon.cola.archetype.source.webopen.application.exceptions.OrganizationFailureType;
import top.egon.cola.archetype.source.webopen.application.user.manage.UserManage;
import top.egon.cola.archetype.source.webopen.application.user.query.UserDetailQuery;
import top.egon.cola.archetype.source.webopen.application.user.result.UserDetailResult;
import top.egon.cola.archetype.source.webopen.application.user.validators.UserApplicationValidator;
import top.egon.cola.archetype.source.webopen.domain.user.entities.User;
import top.egon.cola.archetype.source.webopen.domain.client.CommandIdempotencyPort;
import top.egon.cola.archetype.source.webopen.domain.client.OrganizationEventPublisher;
import top.egon.cola.archetype.source.webopen.domain.user.events.UserChangedEvent;
import top.egon.cola.archetype.source.webopen.domain.user.client.UserCachePort;
import top.egon.cola.archetype.source.webopen.application.support.IdempotentCommand;
import top.egon.cola.archetype.source.webopen.application.support.OrganizationTransactionHooks;
import top.egon.cola.archetype.source.webopen.domain.user.service.UserDomainService;
import top.egon.cola.archetype.source.webopen.domain.user.vos.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;

import java.time.Instant;

@Service("userManage")
@RequiredArgsConstructor
public class UserManageImpl implements UserManage {

    private final UserDomainService<?> userDomainService;
    private final UserApplicationValidator validator;
    private final UserAssembler assembler;
    private final UserCachePort userCache;
    private final CommandIdempotencyPort idempotency;
    private final OrganizationEventPublisher eventPublisher;
    private final LongIdGenerator idGenerator;

    @Override
    @Transactional
    public UserDetailResult createUser(CreateUserCommand command) {
        return IdempotentCommand.execute(idempotency, "create-user", command.requestId(), () -> {
            validator.requireOrganizationAdmin();
            String normalizedEmail = validator.normalizedEmail(command.email());
            if (userDomainService.existsByEmail(normalizedEmail)) {
                throw new OrganizationApplicationException(
                    OrganizationFailureType.CONFLICT, "ORG_CONFLICT", "user email already exists");
            }
            User user = userDomainService.save(userDomainService.create(
                new UserId(idGenerator.nextLongId()), command.name(), normalizedEmail));
            OrganizationTransactionHooks.afterCommit(() -> {
                userCache.evict(user.id());
                eventPublisher.publish(new UserChangedEvent(
                    Long.toString(idGenerator.nextLongId()), user.id().value(), Instant.now(), "CREATED"));
            });
            return assembler.toResult(user);
        });
    }

    @Override
    public UserDetailResult getUser(UserDetailQuery query) {
        UserId userId = new UserId(query.userId());
        User user = userCache.findById(userId).orElseGet(() -> {
            User loaded = userDomainService.findById(userId)
            .orElseThrow(() -> new OrganizationApplicationException(
                OrganizationFailureType.NOT_FOUND, "ORG_NOT_FOUND", "user not found"));
            userCache.put(loaded);
            return loaded;
        });
        return assembler.toResult(user);
    }
}
