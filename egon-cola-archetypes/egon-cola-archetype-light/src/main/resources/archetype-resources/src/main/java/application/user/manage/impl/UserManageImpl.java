package ${package}.application.user.manage.impl;

import ${package}.application.user.command.CreateUserCommand;
import ${package}.application.user.convertor.UserApplicationConvertor;
import ${package}.application.user.manage.UserManage;
import ${package}.application.user.manage.UserUseCaseException;
import ${package}.application.user.query.GetUserQuery;
import ${package}.application.user.result.UserResult;
import ${package}.application.user.validators.UserApplicationValidator;
import ${package}.domain.user.entities.User;
import ${package}.domain.user.exceptions.UserDomainException;
import ${package}.domain.user.client.UserCachePort;
import ${package}.domain.user.event.UserEventPublisher;
import ${package}.domain.user.service.UserDomainService;
import ${package}.domain.user.gateway.UserQueryGateway;
import ${package}.domain.user.vos.UserEvent;
import ${package}.domain.user.vos.UserId;
import ${package}.domain.user.vos.UserSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("userManageImpl")
@Lazy
@RequiredArgsConstructor
@Slf4j
public class UserManageImpl implements UserManage {
    @Qualifier("userDomainService")
    private final UserDomainService<?> userDomainService;
    @Qualifier("userQueryGateway")
    private final UserQueryGateway userQueryGateway;
    @Qualifier("userCachePort")
    private final UserCachePort userCachePort;
    @Qualifier("userEventPublisher")
    private final UserEventPublisher userEventPublisher;
    @Qualifier("userApplicationValidator")
    private final UserApplicationValidator applicationValidator;
    @Qualifier("userApplicationConvertor")
    private final UserApplicationConvertor convertor;

    @Override
    @Transactional
    public UserResult create(CreateUserCommand command) {
        applicationValidator.validate(command);
        userQueryGateway.findExternalUser(command.externalId())
                .orElseThrow(() -> new UserUseCaseException(
                        "EXTERNAL_USER_NOT_FOUND", "external user not found"));
        try {
            User saved = userDomainService.save(userDomainService.createUser(
                    command.externalId(), command.name(), command.email()));
            userCachePort.evictUser(saved.id().value());
            userEventPublisher.publish(UserEvent.created(saved.id().value()));
            return convertor.toResult(saved);
        } catch (UserDomainException exception) {
            throw translate(exception);
        }
    }

    @Override
    public UserResult get(GetUserQuery query) {
        return userCachePort.getUser(query.userId())
                .map(convertor::toResult)
                .orElseGet(() -> loadAndCache(query.userId()));
    }

    private UserResult loadAndCache(Long userId) {
        User user = userDomainService.findById(new UserId(userId))
                .orElseThrow(() -> new UserUseCaseException("USER_NOT_FOUND", "user not found"));
        UserSnapshot snapshot = convertor.toSnapshot(user);
        userCachePort.putUser(snapshot);
        return convertor.toResult(user);
    }

    private UserUseCaseException translate(UserDomainException exception) {
        return new UserUseCaseException(exception.getCode(), exception.getMessage(), exception);
    }
}
