package ${package}.domain.repos.user;

import ${package}.domain.entities.user.Role;
import ${package}.domain.vos.user.RoleCode;

import java.util.Optional;

public interface RoleRepository {
    Optional<Role> findByCode(RoleCode code);
    Role save(Role role);
}
