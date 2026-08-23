package ${package}.domain.user.repos;

import ${package}.domain.user.entities.Permission;
import ${package}.domain.user.vos.PermissionCode;
import ${package}.domain.user.vos.UserId;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository {
    Optional<Permission> findByCode(PermissionCode code);
    List<Permission> findByUserId(UserId userId);
    Permission save(Permission permission);
}
