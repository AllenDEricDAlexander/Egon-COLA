package ${package}.domain.repos.user;

import ${package}.domain.entities.user.Permission;
import ${package}.domain.vos.user.PermissionCode;
import ${package}.domain.vos.user.UserId;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository {
    Optional<Permission> findByCode(PermissionCode code);
    List<Permission> findByUserId(UserId userId);
    Permission save(Permission permission);
}
