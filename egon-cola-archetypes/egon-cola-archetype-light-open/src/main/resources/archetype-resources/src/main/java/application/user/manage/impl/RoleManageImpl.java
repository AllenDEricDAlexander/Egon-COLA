package ${package}.application.user.manage.impl;

import ${package}.application.user.command.AssignRoleCommand;
import ${package}.application.user.convertor.UserApplicationConvertor;
import ${package}.application.user.manage.RoleManage;
import ${package}.application.user.manage.UserUseCaseException;
import ${package}.application.user.result.UserResult;
import ${package}.application.user.validators.UserApplicationValidator;
import ${package}.domain.user.aggregates.UserAggregate;
import ${package}.domain.user.entities.Role;
import ${package}.domain.user.entities.User;
import ${package}.domain.user.exceptions.UserDomainException;
import ${package}.domain.user.repos.RoleRepository;
import ${package}.domain.user.repos.UserRepository;
import ${package}.domain.user.service.RoleDomainService;
import ${package}.domain.user.service.UserCacheService;
import ${package}.domain.user.service.UserEventPublisher;
import ${package}.domain.user.vos.RoleCode;
import ${package}.domain.user.vos.UserEvent;
import ${package}.domain.user.vos.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Lazy
@RequiredArgsConstructor
public class RoleManageImpl implements RoleManage {
    @Qualifier("roleDomainService")
    private final RoleDomainService roleDomainService;
    @Qualifier("userRepository")
    private final UserRepository userRepository;
    @Qualifier("roleRepository")
    private final RoleRepository roleRepository;
    @Qualifier("userCacheService")
    private final UserCacheService userCacheService;
    @Qualifier("userEventPublisher")
    private final UserEventPublisher userEventPublisher;
    private final UserApplicationValidator applicationValidator;
    private final UserApplicationConvertor convertor;

    @Override
    @Transactional
    public UserResult assignRole(AssignRoleCommand command) {
        applicationValidator.validate(command);
        User user = userRepository.findById(new UserId(command.userId()))
                .orElseThrow(() -> new UserUseCaseException("USER_NOT_FOUND", "user not found"));
        Role role = roleRepository.findByCode(new RoleCode(command.roleCode()))
                .orElseThrow(() -> new UserUseCaseException("ROLE_NOT_FOUND", "role not found"));
        try {
            UserAggregate aggregate = roleDomainService.assignRole(new UserAggregate(user), role);
            userRepository.saveRoles(aggregate);
            userCacheService.evictUser(user.id().value());
            userEventPublisher.publish(UserEvent.roleAssigned(user.id().value()));
            return convertor.toResult(user);
        } catch (UserDomainException exception) {
            throw new UserUseCaseException(exception.getCode(), exception.getMessage(), exception);
        }
    }
}
