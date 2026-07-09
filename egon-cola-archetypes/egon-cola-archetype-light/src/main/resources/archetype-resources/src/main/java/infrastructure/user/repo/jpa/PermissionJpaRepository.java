package ${package}.infrastructure.user.repo.jpa;

import ${package}.infrastructure.user.repo.po.PermissionPO;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionJpaRepository extends JpaRepository<PermissionPO, String> {
}
