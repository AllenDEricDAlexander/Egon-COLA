package ${package}.domain.user.service;

import ${package}.domain.user.aggregates.UserAggregate;
import ${package}.domain.user.entities.User;
import ${package}.domain.user.vos.UserId;
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
