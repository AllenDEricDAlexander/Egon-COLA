package ${package}.domain.user.repos;

import ${package}.domain.user.entities.Role;
import ${package}.domain.user.vos.RoleCode;

import java.util.Optional;

public interface RoleRepository {
    Optional<Role> findByCode(RoleCode code);
    Role save(Role role);
}
