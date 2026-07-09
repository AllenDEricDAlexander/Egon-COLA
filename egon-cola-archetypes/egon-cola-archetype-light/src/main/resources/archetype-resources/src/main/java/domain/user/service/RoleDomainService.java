package ${package}.domain.user.service;

import ${package}.domain.user.aggregates.UserAggregate;
import ${package}.domain.user.entities.Role;

public interface RoleDomainService {
    UserAggregate assignRole(UserAggregate user, Role role);
}
