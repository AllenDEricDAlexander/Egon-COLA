package ${package}.domain.user.service;

import ${package}.domain.user.entities.User;
import ${package}.domain.user.vos.UserId;

public interface UserDomainService {

    User create(UserId userId, String name, String email);

}
