package top.egon.cola.archetype.source.light.domain.user.service;

import top.egon.cola.archetype.source.light.domain.user.aggregates.UserAggregate;
import top.egon.cola.archetype.source.light.domain.user.entities.User;
import top.egon.cola.archetype.source.light.domain.user.vos.UserId;
import top.egon.cola.component.common.mybatis.extension.EgonColaIService;
import top.egon.cola.component.common.mybatis.model.EgonModel;

import java.util.Optional;

/** Persistence-owning user domain service contract. */
public interface UserDomainService<P extends EgonModel<P>> extends EgonColaIService<P> {
    User createUser(String externalId, String name, String email);

    User save(User user);

    Optional<User> findById(UserId userId);

    void saveRoles(UserAggregate aggregate);
}
