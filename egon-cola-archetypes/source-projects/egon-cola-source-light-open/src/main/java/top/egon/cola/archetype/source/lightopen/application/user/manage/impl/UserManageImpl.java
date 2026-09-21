package top.egon.cola.archetype.source.lightopen.application.user.manage.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.archetype.source.lightopen.application.user.manage.UserManage;
import top.egon.cola.archetype.source.lightopen.application.user.pojo.command.CreateUserCommand;
import top.egon.cola.archetype.source.lightopen.application.user.pojo.convertor.UserApplicationConvertor;
import top.egon.cola.archetype.source.lightopen.application.user.pojo.query.GetUserQuery;
import top.egon.cola.archetype.source.lightopen.application.user.pojo.result.UserResult;
import top.egon.cola.archetype.source.lightopen.application.user.validators.UserApplicationValidator;
import top.egon.cola.archetype.source.lightopen.common.exception.UserDomainException;
import top.egon.cola.archetype.source.lightopen.common.exception.UserUseCaseException;
import top.egon.cola.archetype.source.lightopen.domain.user.entities.User;
import top.egon.cola.archetype.source.lightopen.domain.user.service.UserDomainService;
import top.egon.cola.archetype.source.lightopen.domain.user.service.UserEventService;
import top.egon.cola.archetype.source.lightopen.domain.user.service.UserIdempotencyService;
import top.egon.cola.archetype.source.lightopen.domain.user.service.UserQueryService;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.UserEvent;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.UserId;

@Service("userManageImpl")
@Lazy
@RequiredArgsConstructor
@Slf4j
public class UserManageImpl implements UserManage {
    @Qualifier("userDomainService")
    private final UserDomainService userDomainService;
    @Qualifier("userQueryService")
    private final UserQueryService userQueryService;
    @Qualifier("userEventService")
    private final UserEventService userEventService;
    @Qualifier("userIdempotencyService")
    private final UserIdempotencyService userIdempotencyService;
    @Qualifier("userApplicationValidator")
    private final UserApplicationValidator applicationValidator;
    @Qualifier("userApplicationConvertorImpl")
    private final UserApplicationConvertor convertor;

    @Override
    @Transactional
    public UserResult create(CreateUserCommand command) {
        applicationValidator.validate(command);
        claim(command.idempotencyKey());
        userQueryService.findExternalUser(command.externalId())
                .orElseThrow(() -> new UserUseCaseException(
                        "EXTERNAL_USER_NOT_FOUND", "external user not found"));
        try {
            User saved = userDomainService.save(userDomainService.createUser(
                    command.externalId(), command.name(), command.email()));
            userEventService.publish(UserEvent.created(saved.id().value()));
            return convertor.toTarget(saved);
        } catch (UserDomainException exception) {
            throw translate(exception);
        }
    }

    @Override
    public UserResult get(GetUserQuery query) {
        return userDomainService.findById(new UserId(query.userId()))
                .map(convertor::toTarget)
                .orElseThrow(() -> new UserUseCaseException("USER_NOT_FOUND", "user not found"));
    }

    private void claim(String idempotencyKey) {
        if (!userIdempotencyService.claim(idempotencyKey)) {
            throw new UserUseCaseException("DUPLICATE_REQUEST", "request was already processed");
        }
    }

    private UserUseCaseException translate(UserDomainException exception) {
        return new UserUseCaseException(exception.getStatus(), exception.getMessage(), exception);
    }
}
