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
import ${package}.domain.user.repos.UserRepository;
import ${package}.domain.user.service.UserCacheService;
import ${package}.domain.user.service.UserDomainService;
import ${package}.domain.user.service.UserEventPublisher;
import ${package}.domain.user.service.UserQueryService;
import ${package}.domain.user.vos.UserEvent;
import ${package}.domain.user.vos.UserId;
import ${package}.domain.user.vos.UserSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Lazy
@RequiredArgsConstructor
public class UserManageImpl implements UserManage {
    @Qualifier("userDomainService")
    private final UserDomainService userDomainService;
    @Qualifier("userRepository")
    private final UserRepository userRepository;
    @Qualifier("userQueryService")
    private final UserQueryService userQueryService;
    @Qualifier("userCacheService")
    private final UserCacheService userCacheService;
    @Qualifier("userEventPublisher")
    private final UserEventPublisher userEventPublisher;
    private final UserApplicationValidator applicationValidator;
    private final UserApplicationConvertor convertor;

    @Override
    @Transactional
    public UserResult create(CreateUserCommand command) {
        applicationValidator.validate(command);
        userQueryService.findExternalUser(command.externalId())
                .orElseThrow(() -> new UserUseCaseException(
                        "EXTERNAL_USER_NOT_FOUND", "external user not found"));
        try {
            User saved = userRepository.save(userDomainService.createUser(
                    command.externalId(), command.name(), command.email()));
            userCacheService.evictUser(saved.id().value());
            userEventPublisher.publish(UserEvent.created(saved.id().value()));
            return convertor.toResult(saved);
        } catch (UserDomainException exception) {
            throw translate(exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResult get(GetUserQuery query) {
        return userCacheService.getUser(query.userId())
                .map(convertor::toResult)
                .orElseGet(() -> loadAndCache(query.userId()));
    }

    private UserResult loadAndCache(String userId) {
        User user = userRepository.findById(new UserId(userId))
                .orElseThrow(() -> new UserUseCaseException("USER_NOT_FOUND", "user not found"));
        UserSnapshot snapshot = convertor.toSnapshot(user);
        userCacheService.putUser(snapshot);
        return convertor.toResult(user);
    }

    private UserUseCaseException translate(UserDomainException exception) {
        return new UserUseCaseException(exception.getCode(), exception.getMessage(), exception);
    }
}
