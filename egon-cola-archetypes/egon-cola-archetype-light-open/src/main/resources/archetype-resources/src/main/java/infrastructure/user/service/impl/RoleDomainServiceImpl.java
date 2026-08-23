package ${package}.infrastructure.user.service.impl;

import ${package}.domain.user.aggregates.UserAggregate;
import ${package}.domain.user.entities.Role;
import ${package}.domain.user.service.RoleDomainService;
import org.springframework.stereotype.Service;

@Service("roleDomainService")
public class RoleDomainServiceImpl implements RoleDomainService {
    @Override
    public UserAggregate assignRole(UserAggregate user, Role role) {
        user.assign(role);
        return user;
    }
}
