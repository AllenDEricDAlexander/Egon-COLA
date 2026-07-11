package ${package}.infrastructure.repo.user.jpa;

import ${package}.infrastructure.repo.user.po.PermissionPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionJpaRepository extends JpaRepository<PermissionPO, String> {
    Optional<PermissionPO> findByCode(String code);
}
