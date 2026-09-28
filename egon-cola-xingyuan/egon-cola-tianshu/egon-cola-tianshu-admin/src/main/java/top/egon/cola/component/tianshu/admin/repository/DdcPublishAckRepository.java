package top.egon.cola.component.tianshu.admin.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.tianshu.admin.model.bo.DdcPublishAckBO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Validated
public interface DdcPublishAckRepository {

    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcPublishAckBO> findByChangeIdAndInstanceIdAndLeaseId(String changeId, String instanceId, String leaseId);

    @Validated(EgonColaModelValidationGroups.Query.class)
    Optional<DdcPublishAckBO> findForUpdateByChangeIdAndInstanceIdAndLeaseId(
            String changeId, String instanceId, String leaseId);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<DdcPublishAckBO> findByChangeId(String changeId);

    @Validated(EgonColaModelValidationGroups.Update.class)
    int markIncompleteTimeout(String changeId, String timeoutStatus, LocalDateTime ackAt);

    @Validated(EgonColaModelValidationGroups.Update.class)
    int resetTargets(String changeId);

    @Validated(EgonColaModelValidationGroups.Insert.class)
    DdcPublishAckBO insert(@Valid @NotNull(groups = EgonColaModelValidationGroups.Insert.class) DdcPublishAckBO command);

    @Validated(EgonColaModelValidationGroups.Insert.class)
    List<DdcPublishAckBO> insertAll(List<@Valid DdcPublishAckBO> commands);

    @Validated(EgonColaModelValidationGroups.Update.class)
    DdcPublishAckBO update(@Valid @NotNull(groups = EgonColaModelValidationGroups.Update.class) DdcPublishAckBO command);
}
