package ${package}.infrastructure.user.repo.jpa;

import ${package}.infrastructure.user.repo.po.RolePermissionPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolePermissionJpaRepository extends JpaRepository<RolePermissionPO, Long> {
    List<RolePermissionPO> findByRoleId(String roleId);
    List<RolePermissionPO> findByRoleIdIn(List<String> roleIds);
    boolean existsByRoleIdAndPermissionId(String roleId, String permissionId);
}
