package top.egon.cola.archetype.source.webopen.application.user.manage.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.archetype.source.webopen.application.support.IdempotentCommand;
import top.egon.cola.archetype.source.webopen.application.support.OrganizationTransactionHooks;
import top.egon.cola.archetype.source.webopen.application.user.manage.UserManage;
import top.egon.cola.archetype.source.webopen.application.user.pojo.command.CreateUserCommand;
import top.egon.cola.archetype.source.webopen.application.user.pojo.convertor.UserConverter;
import top.egon.cola.archetype.source.webopen.application.user.pojo.query.UserDetailQuery;
import top.egon.cola.archetype.source.webopen.application.user.pojo.result.UserDetailResult;
import top.egon.cola.archetype.source.webopen.application.user.validators.UserApplicationValidator;
import top.egon.cola.archetype.source.webopen.common.enums.OrganizationFailureType;
import top.egon.cola.archetype.source.webopen.common.exception.OrganizationApplicationException;
import top.egon.cola.archetype.source.webopen.domain.service.CommandIdempotencyService;
import top.egon.cola.archetype.source.webopen.domain.service.OrganizationEventService;
import top.egon.cola.archetype.source.webopen.domain.user.entities.User;
import top.egon.cola.archetype.source.webopen.domain.user.events.UserChangedEvent;
import top.egon.cola.archetype.source.webopen.domain.user.service.UserDomainService;
import top.egon.cola.archetype.source.webopen.domain.user.vos.UserId;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;

import java.time.Instant;

/** User use cases; caching and transport stay inside the domain service implementations. */
@Service("userManage")
@Validated
@Slf4j
@RequiredArgsConstructor
public class UserManageImpl implements UserManage {

    @Qualifier("userDomainService")
    private final UserDomainService userDomainService;
    @Qualifier("userApplicationValidator")
    private final UserApplicationValidator validator;
    @Qualifier("userConverterImpl")
    private final UserConverter converter;
    @Qualifier("commandIdempotencyService")
    private final CommandIdempotencyService idempotency;
    @Qualifier("organizationEventService")
    private final OrganizationEventService eventService;

    @Override
    @Transactional
    public UserDetailResult createUser(CreateUserCommand command) {
        return IdempotentCommand.execute(idempotency, "create-user", command.requestId(), () -> {
            validator.requireOrganizationAdmin();
            String normalizedEmail = validator.normalizedEmail(command.email());
            if (userDomainService.existsByEmail(normalizedEmail)) {
                throw conflict("user email already exists");
            }
            User user = userDomainService.save(userDomainService.create(
                    new UserId(SnowflakeIdGenerator.nextLongId()), command.name(), normalizedEmail));
            OrganizationTransactionHooks.afterCommit(() -> eventService.publish(
                    new UserChangedEvent(
                            Long.toString(SnowflakeIdGenerator.nextLongId()),
                            user.id().value(), Instant.now(), "CREATED")));
            return converter.toResult(user);
        });
    }

    @Override
    public UserDetailResult getUser(UserDetailQuery query) {
        User user = userDomainService.findById(new UserId(query.userId()))
                .orElseThrow(() -> notFound("user not found"));
        return converter.toResult(user);
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
