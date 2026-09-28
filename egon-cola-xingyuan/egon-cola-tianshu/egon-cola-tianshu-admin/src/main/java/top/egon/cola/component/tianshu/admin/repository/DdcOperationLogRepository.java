package top.egon.cola.component.tianshu.admin.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.tianshu.admin.model.bo.DdcOperationLogBO;

import java.util.List;

@Validated
public interface DdcOperationLogRepository {

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcOperationLogBO> findByBizCodeAndEnvAndAppCode(String bizCode, String env, String appCode);

    @Validated(EgonColaModelValidationGroups.Insert.class)
    DdcOperationLogBO insert(@Valid @NotNull(groups = EgonColaModelValidationGroups.Insert.class)
            DdcOperationLogBO command);
}
