package ${package}.application.manage.user.impl;

import ${package}.application.assemblers.user.UserAssembler;
import ${package}.application.command.user.CreateUserCommand;
import ${package}.application.exceptions.OrganizationApplicationException;
import ${package}.application.exceptions.OrganizationFailureType;
import ${package}.application.manage.user.UserManage;
import ${package}.application.query.user.UserDetailQuery;
import ${package}.application.result.user.UserDetailResult;
import ${package}.application.validators.user.UserApplicationValidator;
import ${package}.domain.entities.user.User;
import ${package}.domain.repos.user.UserRepository;
import ${package}.domain.service.user.UserDomainService;
import ${package}.domain.vos.user.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service("userManage")
public class UserManageImpl implements UserManage {

    private final UserRepository userRepository;
    private final UserDomainService userDomainService;
    private final UserApplicationValidator validator;
    private final UserAssembler assembler;

    public UserManageImpl(
            UserRepository userRepository,
            UserDomainService userDomainService,
            UserApplicationValidator validator,
            UserAssembler assembler) {
        this.userRepository = userRepository;
        this.userDomainService = userDomainService;
        this.validator = validator;
        this.assembler = assembler;
    }

    @Override
    @Transactional
    public UserDetailResult createUser(CreateUserCommand command) {
        validator.requireOrganizationAdmin();
        String normalizedEmail = validator.normalizedEmail(command.email());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new OrganizationApplicationException(
                OrganizationFailureType.CONFLICT, "ORG_CONFLICT", "user email already exists");
        }
        User user = userDomainService.create(
            new UserId("user-" + UUID.randomUUID().toString().toLowerCase()), command.name(), normalizedEmail);
        return assembler.toResult(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailResult getUser(UserDetailQuery query) {
        User user = userRepository.findById(new UserId(query.userId()))
            .orElseThrow(() -> new OrganizationApplicationException(
                OrganizationFailureType.NOT_FOUND, "ORG_NOT_FOUND", "user not found"));
        return assembler.toResult(user);
    }
}
