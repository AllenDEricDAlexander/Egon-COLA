package ${package}.application.manage.user.impl;

import ${package}.application.command.user.AssignRoleCommand;
import ${package}.application.exceptions.OrganizationApplicationException;
import ${package}.application.exceptions.OrganizationFailureType;
import ${package}.application.manage.user.RoleManage;
import ${package}.application.validators.user.UserApplicationValidator;
import ${package}.domain.aggregates.user.UserAggregate;
import ${package}.domain.entities.user.Role;
import ${package}.domain.entities.user.User;
import ${package}.domain.repos.user.RoleRepository;
import ${package}.domain.repos.user.UserRepository;
import ${package}.domain.vos.user.RoleCode;
import ${package}.domain.vos.user.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("roleManage")
public class RoleManageImpl implements RoleManage {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserApplicationValidator validator;

    public RoleManageImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserApplicationValidator validator) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.validator = validator;
    }

    @Override
    @Transactional
    public void assignRole(AssignRoleCommand command) {
        validator.requireOrganizationAdmin();
        User user = userRepository.findById(new UserId(command.userId()))
            .orElseThrow(() -> notFound("user not found"));
        Role role = roleRepository.findByCode(new RoleCode(command.roleCode()))
            .orElseThrow(() -> notFound("role not found"));
        UserAggregate aggregate = new UserAggregate(user);
        aggregate.assignRole(role);
        userRepository.save(aggregate.user());
    }

    private static OrganizationApplicationException notFound(String message) {
        return new OrganizationApplicationException(
            OrganizationFailureType.NOT_FOUND, "ORG_NOT_FOUND", message);
    }
}
