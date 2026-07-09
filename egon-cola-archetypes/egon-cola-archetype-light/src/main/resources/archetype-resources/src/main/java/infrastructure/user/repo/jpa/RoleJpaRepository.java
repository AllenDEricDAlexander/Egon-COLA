package ${package}.infrastructure.user.repo.jpa;

import ${package}.infrastructure.user.repo.po.RolePO;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleJpaRepository extends JpaRepository<RolePO, String> {
}
