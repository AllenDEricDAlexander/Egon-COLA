package ${package}.infrastructure.repo.user.jpa;

import ${package}.infrastructure.repo.user.po.UserRolePO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleJpaRepository extends JpaRepository<UserRolePO, Long> {
    List<UserRolePO> findByUserId(String userId);
    List<UserRolePO> findByRoleIdIn(List<String> roleIds);
    boolean existsByUserIdAndRoleId(String userId, String roleId);
}
