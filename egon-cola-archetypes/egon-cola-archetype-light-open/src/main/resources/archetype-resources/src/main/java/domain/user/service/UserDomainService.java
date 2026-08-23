package ${package}.domain.user.service;

import ${package}.domain.user.entities.User;

public interface UserDomainService {
    User createUser(String externalId, String name, String email);
}
