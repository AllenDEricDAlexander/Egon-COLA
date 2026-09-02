package top.egon.cola.archetype.source.web.domain.user.service;

import top.egon.cola.archetype.source.web.domain.user.entities.Role;
import top.egon.cola.archetype.source.web.domain.user.entities.User;
import top.egon.cola.archetype.source.web.domain.user.vos.RoleCode;
import top.egon.cola.archetype.source.web.domain.user.vos.UserId;
import top.egon.cola.component.common.mybatis.extension.EgonColaIService;
import top.egon.cola.component.common.mybatis.model.EgonModel;

import java.util.Optional;

public interface UserDomainService<P extends EgonModel<P>> extends EgonColaIService<P> {
    User create(UserId userId, String name, String email);

    User save(User user);

    Optional<User> findById(UserId userId);

    boolean existsByEmail(String normalizedEmail);

    Optional<Role> findRoleByCode(RoleCode code);

    Role saveRole(Role role);
}
