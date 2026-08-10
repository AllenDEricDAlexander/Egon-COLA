package top.egon.cola.component.ddc.admin.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigVersionEntity;

import java.util.List;
import java.util.Optional;

public interface DdcConfigVersionRepository extends JpaRepository<DdcConfigVersionEntity, String> {

    List<DdcConfigVersionEntity> findByConfigIdOrderByVersionDesc(String configId);

    Page<DdcConfigVersionEntity> findByConfigIdOrderByVersionDescIdDesc(
            String configId,
            Pageable pageable);

    Optional<DdcConfigVersionEntity> findByConfigIdAndVersion(String configId, Long version);

    List<DdcConfigVersionEntity> findByBizCodeAndEnvAndAppCodeAndResourceName(
            String bizCode, String env, String appCode, String resourceName);
}
