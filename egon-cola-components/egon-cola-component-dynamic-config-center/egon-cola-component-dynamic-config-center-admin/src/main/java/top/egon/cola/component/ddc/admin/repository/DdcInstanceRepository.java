package top.egon.cola.component.ddc.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.cola.component.ddc.admin.model.entity.DdcInstanceEntity;

import java.util.List;
import java.util.Optional;

public interface DdcInstanceRepository extends JpaRepository<DdcInstanceEntity, String> {

    Optional<DdcInstanceEntity> findByInstanceId(String instanceId);

    List<DdcInstanceEntity> findByAppCodeAndEnvAndNamespace(String appCode, String env, String namespace);
}
