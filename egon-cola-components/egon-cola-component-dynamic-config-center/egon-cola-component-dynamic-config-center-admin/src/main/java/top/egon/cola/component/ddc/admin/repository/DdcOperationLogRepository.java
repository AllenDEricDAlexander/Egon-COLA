package top.egon.cola.component.ddc.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.cola.component.ddc.admin.model.entity.DdcOperationLogEntity;

import java.util.List;

public interface DdcOperationLogRepository extends JpaRepository<DdcOperationLogEntity, String> {

    List<DdcOperationLogEntity> findByAppCodeAndEnvAndNamespace(String appCode, String env, String namespace);
}
