package ${package}.infrastructure.user.repo.jpa;

import ${package}.infrastructure.user.repo.po.UserRolePO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleJpaRepository extends JpaRepository<UserRolePO, Long> {
    List<UserRolePO> findByUserId(Long userId);
    List<UserRolePO> findByRoleIdIn(List<Long> roleIds);
    long countByUserIdAndRoleId(Long userId, Long roleId);
}
