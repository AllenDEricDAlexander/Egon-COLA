package ${package}.infrastructure.user.repo.jpa;

import ${package}.infrastructure.user.repo.po.UserRolePO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleJpaRepository extends JpaRepository<UserRolePO, String> {
    List<UserRolePO> findByUserId(String userId);
    List<UserRolePO> findByRoleIdIn(List<String> roleIds);
    boolean existsByUserIdAndRoleId(String userId, String roleId);
}
