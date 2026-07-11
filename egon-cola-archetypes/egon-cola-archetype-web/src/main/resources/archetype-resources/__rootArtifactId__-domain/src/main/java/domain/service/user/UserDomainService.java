package ${package}.domain.service.user;

import ${package}.domain.entities.user.User;
import ${package}.domain.vos.user.UserId;

public interface UserDomainService {

    User create(UserId userId, String name, String email);

    User assignClass(User user, String schoolClassId);
}
