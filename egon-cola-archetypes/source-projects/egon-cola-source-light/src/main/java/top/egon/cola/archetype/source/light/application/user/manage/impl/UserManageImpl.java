package top.egon.cola.archetype.source.light.application.user.manage.impl;

import top.egon.cola.archetype.source.light.application.user.command.CreateUserCommand;
import top.egon.cola.archetype.source.light.application.user.convertor.UserApplicationConvertor;
import top.egon.cola.archetype.source.light.application.user.manage.UserManage;
import top.egon.cola.archetype.source.light.application.user.manage.UserUseCaseException;
import top.egon.cola.archetype.source.light.application.user.query.GetUserQuery;
import top.egon.cola.archetype.source.light.application.user.result.UserResult;
import top.egon.cola.archetype.source.light.application.user.validators.UserApplicationValidator;
import top.egon.cola.archetype.source.light.domain.user.entities.User;
import top.egon.cola.archetype.source.light.domain.user.exceptions.UserDomainException;
import top.egon.cola.archetype.source.light.domain.user.client.UserCachePort;
import top.egon.cola.archetype.source.light.domain.user.event.UserEventPublisher;
import top.egon.cola.archetype.source.light.domain.user.service.UserDomainService;
import top.egon.cola.archetype.source.light.domain.user.gateway.UserQueryGateway;
import top.egon.cola.archetype.source.light.domain.user.vos.UserEvent;
import top.egon.cola.archetype.source.light.domain.user.vos.UserId;
import top.egon.cola.archetype.source.light.domain.user.vos.UserSnapshot;
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
