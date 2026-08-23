package ${package}.infrastructure.user.repo.jpa;

import ${package}.infrastructure.user.repo.po.UserRolePO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleJpaRepository extends JpaRepository<UserRolePO, UserRolePO.Key> {
    List<UserRolePO> findByUserId(String userId);
}
