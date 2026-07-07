package top.egon.cola.component.ddc.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigVersionEntity;

import java.util.List;

public interface DdcConfigVersionRepository extends JpaRepository<DdcConfigVersionEntity, String> {

    List<DdcConfigVersionEntity> findByConfigIdOrderByVersionDesc(String configId);

    List<DdcConfigVersionEntity> findByAppCodeAndEnvAndNamespaceAndConfigKey(String appCode, String env, String namespace, String configKey);
}
