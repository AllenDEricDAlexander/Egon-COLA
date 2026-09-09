package top.egon.cola.component.tianshu.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.cola.component.tianshu.admin.model.entity.DdcOperationLogEntity;

import java.util.List;

public interface DdcOperationLogRepository extends JpaRepository<DdcOperationLogEntity, String> {

    List<DdcOperationLogEntity> findByBizCodeAndEnvAndAppCode(
            String bizCode, String env, String appCode);
}
