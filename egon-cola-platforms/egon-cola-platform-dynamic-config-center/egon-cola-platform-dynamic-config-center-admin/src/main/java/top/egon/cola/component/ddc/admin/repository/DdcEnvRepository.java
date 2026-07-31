package top.egon.cola.component.ddc.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.cola.component.ddc.admin.model.entity.DdcEnvEntity;

import java.util.List;
import java.util.Optional;

public interface DdcEnvRepository extends JpaRepository<DdcEnvEntity, String> {

    Optional<DdcEnvEntity> findByEnvCode(String envCode);

    boolean existsByEnvCode(String envCode);

    boolean existsByEnvCodeAndIdNot(String envCode, String id);

    List<DdcEnvEntity> findAllByOrderBySortOrderAsc();

    List<DdcEnvEntity> findByEnvCodeContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String envCode, String description);
}
