package ${package}.infrastructure.user.repo.jpa;

import ${package}.infrastructure.user.repo.po.RolePermissionPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolePermissionJpaRepository
        extends JpaRepository<RolePermissionPO, RolePermissionPO.Key> {
    List<RolePermissionPO> findByRoleCode(String roleCode);
}
