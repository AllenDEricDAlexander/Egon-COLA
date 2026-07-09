package ${package}.infrastructure.user.repo.jpa;

import ${package}.infrastructure.user.repo.po.PermissionPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PermissionJpaRepository extends JpaRepository<PermissionPO, String> {
    List<PermissionPO> findByCodeInOrderByCode(Collection<String> codes);
}
