package top.egon.cola.archetype.source.lightopen.application.user.manage.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.archetype.source.lightopen.application.user.manage.RoleManage;
import top.egon.cola.archetype.source.lightopen.application.user.pojo.command.AssignRoleCommand;
import top.egon.cola.archetype.source.lightopen.application.user.pojo.convertor.UserApplicationConvertor;
import top.egon.cola.archetype.source.lightopen.application.user.pojo.result.UserResult;
import top.egon.cola.archetype.source.lightopen.application.user.validators.UserApplicationValidator;
import top.egon.cola.archetype.source.lightopen.common.exception.UserDomainException;
import top.egon.cola.archetype.source.lightopen.common.exception.UserUseCaseException;
import top.egon.cola.archetype.source.lightopen.domain.user.aggregates.UserAggregate;
import top.egon.cola.archetype.source.lightopen.domain.user.entities.Role;
import top.egon.cola.archetype.source.lightopen.domain.user.entities.User;
import top.egon.cola.archetype.source.lightopen.domain.user.service.RoleDomainService;
import top.egon.cola.archetype.source.lightopen.domain.user.service.UserDomainService;
import top.egon.cola.archetype.source.lightopen.domain.user.service.UserEventService;
import top.egon.cola.archetype.source.lightopen.domain.user.service.UserIdempotencyService;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.RoleCode;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.UserEvent;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.UserId;

@Service("roleManageImpl")
@Lazy
@RequiredArgsConstructor
@Slf4j
public class RoleManageImpl implements RoleManage {
    @Qualifier("roleDomainService")
    private final RoleDomainService roleDomainService;
    @Qualifier("userDomainService")
    private final UserDomainService userDomainService;
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
    public UserResult assignRole(AssignRoleCommand command) {
        applicationValidator.validate(command);
        if (!userIdempotencyService.claim(command.idempotencyKey())) {
            throw new UserUseCaseException("DUPLICATE_REQUEST", "request was already processed");
        }
        User user = userDomainService.findById(new UserId(command.userId()))
                .orElseThrow(() -> new UserUseCaseException("USER_NOT_FOUND", "user not found"));
        Role role = roleDomainService.findByCode(new RoleCode(command.roleCode()))
                .orElseThrow(() -> new UserUseCaseException("ROLE_NOT_FOUND", "role not found"));
        try {
            UserAggregate aggregate = roleDomainService.assignRole(new UserAggregate(user), role);
            userDomainService.saveRoles(aggregate);
            userEventService.publish(UserEvent.roleAssigned(user.id().value()));
            return convertor.toTarget(user);
        } catch (UserDomainException exception) {
            throw new UserUseCaseException(exception.getStatus(), exception.getMessage(), exception);
        }
    }
}
