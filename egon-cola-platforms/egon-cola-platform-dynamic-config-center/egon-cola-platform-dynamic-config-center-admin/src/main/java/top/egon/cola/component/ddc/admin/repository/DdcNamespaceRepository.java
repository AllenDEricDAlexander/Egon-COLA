package top.egon.cola.component.ddc.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.cola.component.ddc.admin.model.entity.DdcNamespaceEntity;

import java.util.List;
import java.util.Optional;

public interface DdcNamespaceRepository extends JpaRepository<DdcNamespaceEntity, String> {

    Optional<DdcNamespaceEntity> findByAppCodeAndEnvAndNamespace(String appCode, String env, String namespace);

    List<DdcNamespaceEntity> findByAppCodeAndEnv(String appCode, String env);
}
