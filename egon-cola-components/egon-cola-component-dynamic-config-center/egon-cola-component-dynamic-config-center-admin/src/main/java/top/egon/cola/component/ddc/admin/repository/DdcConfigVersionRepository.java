package top.egon.cola.component.ddc.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigVersionEntity;

import java.util.List;
import java.util.Optional;

public interface DdcConfigVersionRepository extends JpaRepository<DdcConfigVersionEntity, String> {

    List<DdcConfigVersionEntity> findByConfigIdOrderByVersionDesc(String configId);

    Optional<DdcConfigVersionEntity> findByConfigIdAndVersion(String configId, Long version);

    List<DdcConfigVersionEntity> findByAppCodeAndEnvAndNamespaceAndConfigKey(String appCode, String env, String namespace, String configKey);
}
