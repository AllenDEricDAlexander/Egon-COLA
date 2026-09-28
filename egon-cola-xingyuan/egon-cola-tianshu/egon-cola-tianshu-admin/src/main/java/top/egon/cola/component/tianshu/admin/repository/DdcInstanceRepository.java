package top.egon.cola.component.tianshu.admin.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.tianshu.admin.model.bo.DdcInstanceBO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Validated
public interface DdcInstanceRepository {

    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcInstanceBO> findByInstanceId(String instanceId);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcInstanceBO> findByBizCodeAndEnvAndAppCode(String bizCode, String env, String appCode);

    @Validated(EgonColaModelValidationGroups.Query.class)
    DdcPageResult<DdcInstanceBO> findByBizCodeAndEnvAndAppCode(
            String bizCode, String env, String appCode, PageQuery pageQuery);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcInstanceBO> findByBizCodeAndEnvAndAppCodeAndStatus(
            String bizCode, String env, String appCode, String status);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcInstanceBO> findByStatusAndLeaseExpireAtLessThanEqual(String status, LocalDateTime leaseExpireAt);

    @Validated(EgonColaModelValidationGroups.Update.class)
    int markOfflineIfLeaseMatches(String instanceId, String leaseId, String status, LocalDateTime updatedAt);

    @Validated(EgonColaModelValidationGroups.Update.class)
    default int markResourceAdmissionOffline(String resourceServerId, String bizCode, String env,
            String appCode, long resourceVersion, Instant updatedAt) {
        return markResourceAdmissionOfflineAt(resourceServerId, bizCode, env, appCode,
                resourceVersion, LocalDateTime.ofInstant(updatedAt, ZoneOffset.UTC));
    }

    @Validated(EgonColaModelValidationGroups.Update.class)
    int markResourceAdmissionOfflineAt(String resourceServerId, String bizCode, String env,
            String appCode, long resourceVersion, LocalDateTime updatedAt);

    @Validated(EgonColaModelValidationGroups.Insert.class)
    DdcInstanceBO insert(@Valid @NotNull(groups = EgonColaModelValidationGroups.Insert.class) DdcInstanceBO command);

    @Validated(EgonColaModelValidationGroups.Update.class)
    DdcInstanceBO update(@Valid @NotNull(groups = EgonColaModelValidationGroups.Update.class) DdcInstanceBO command);
}
