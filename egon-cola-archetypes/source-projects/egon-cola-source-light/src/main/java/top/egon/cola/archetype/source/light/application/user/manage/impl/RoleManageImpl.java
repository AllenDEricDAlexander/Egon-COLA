package top.egon.cola.archetype.source.light.application.user.manage.impl;

import top.egon.cola.archetype.source.light.application.user.command.AssignRoleCommand;
import top.egon.cola.archetype.source.light.application.user.convertor.UserApplicationConvertor;
import top.egon.cola.archetype.source.light.application.user.manage.RoleManage;
import top.egon.cola.archetype.source.light.application.user.manage.UserUseCaseException;
import top.egon.cola.archetype.source.light.application.user.result.UserResult;
import top.egon.cola.archetype.source.light.application.user.validators.UserApplicationValidator;
import top.egon.cola.archetype.source.light.domain.user.aggregates.UserAggregate;
import top.egon.cola.archetype.source.light.domain.user.entities.Role;
import top.egon.cola.archetype.source.light.domain.user.entities.User;
import top.egon.cola.archetype.source.light.domain.user.exceptions.UserDomainException;
import top.egon.cola.archetype.source.light.domain.user.event.UserEventPublisher;
import top.egon.cola.archetype.source.light.domain.user.service.RoleDomainService;
import top.egon.cola.archetype.source.light.domain.user.service.UserDomainService;
import top.egon.cola.archetype.source.light.domain.user.client.UserCachePort;
import top.egon.cola.archetype.source.light.domain.user.vos.RoleCode;
import top.egon.cola.archetype.source.light.domain.user.vos.UserEvent;
import top.egon.cola.archetype.source.light.domain.user.vos.UserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("roleManageImpl")
@Lazy
@RequiredArgsConstructor
@Slf4j
public class RoleManageImpl implements RoleManage {
    @Qualifier("roleDomainService")
    private final RoleDomainService<?> roleDomainService;
    @Qualifier("userDomainService")
    private final UserDomainService<?> userDomainService;
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
    public UserResult assignRole(AssignRoleCommand command) {
        applicationValidator.validate(command);
        User user = userDomainService.findById(new UserId(command.userId()))
                .orElseThrow(() -> new UserUseCaseException("USER_NOT_FOUND", "user not found"));
        Role role = roleDomainService.findByCode(new RoleCode(command.roleCode()))
                .orElseThrow(() -> new UserUseCaseException("ROLE_NOT_FOUND", "role not found"));
        try {
            UserAggregate aggregate = roleDomainService.assignRole(new UserAggregate(user), role);
            userDomainService.saveRoles(aggregate);
            userCachePort.evictUser(user.id().value());
            userEventPublisher.publish(UserEvent.roleAssigned(user.id().value()));
            return convertor.toResult(user);
        } catch (UserDomainException exception) {
            throw new UserUseCaseException(exception.getCode(), exception.getMessage(), exception);
        }
    }
}
