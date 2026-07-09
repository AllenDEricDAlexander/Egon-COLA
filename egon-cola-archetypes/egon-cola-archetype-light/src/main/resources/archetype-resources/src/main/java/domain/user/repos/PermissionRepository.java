package ${package}.domain.user.repos;

import ${package}.domain.user.entities.Permission;
import ${package}.domain.user.vos.PermissionCode;

import java.util.Optional;

public interface PermissionRepository {
    Optional<Permission> findByCode(PermissionCode permissionCode);

    Permission save(Permission permission);
}
