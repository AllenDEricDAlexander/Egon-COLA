package top.egon.cola.component.ddc.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigItemEntity;

import java.util.List;
import java.util.Optional;

public interface DdcConfigItemRepository extends JpaRepository<DdcConfigItemEntity, String> {

    Optional<DdcConfigItemEntity> findByAppCodeAndEnvAndNamespaceAndConfigKey(String appCode, String env, String namespace, String configKey);

    List<DdcConfigItemEntity> findByAppCodeAndEnvAndNamespace(String appCode, String env, String namespace);

    List<DdcConfigItemEntity> findByAppCodeAndEnvAndNamespaceAndDeletedFalse(String appCode, String env, String namespace);
}
