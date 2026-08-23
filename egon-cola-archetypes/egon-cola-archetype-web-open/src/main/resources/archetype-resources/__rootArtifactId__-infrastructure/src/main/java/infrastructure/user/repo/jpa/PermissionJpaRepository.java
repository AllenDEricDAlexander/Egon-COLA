package ${package}.infrastructure.user.repo.jpa;

import ${package}.infrastructure.user.repo.po.PermissionPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionJpaRepository extends JpaRepository<PermissionPO, String> {
    Optional<PermissionPO> findByCode(String code);
}
