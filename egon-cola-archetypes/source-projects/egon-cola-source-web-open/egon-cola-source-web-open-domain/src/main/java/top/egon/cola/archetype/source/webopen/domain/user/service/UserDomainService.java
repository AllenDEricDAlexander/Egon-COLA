package top.egon.cola.archetype.source.webopen.domain.user.service;

import top.egon.cola.archetype.source.webopen.domain.user.entities.Role;
import top.egon.cola.archetype.source.webopen.domain.user.entities.User;
import top.egon.cola.archetype.source.webopen.domain.user.vos.RoleCode;
import top.egon.cola.archetype.source.webopen.domain.user.vos.UserId;

import java.util.Optional;

public interface UserDomainService {
    User create( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull UserId userId, String name, String email);

    User save( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull User user);

    Optional<User> findById( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull UserId userId);

    boolean existsByEmail(String normalizedEmail);

    Optional<Role> findRoleByCode( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull RoleCode code);

    Role saveRole( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull Role role);
}
