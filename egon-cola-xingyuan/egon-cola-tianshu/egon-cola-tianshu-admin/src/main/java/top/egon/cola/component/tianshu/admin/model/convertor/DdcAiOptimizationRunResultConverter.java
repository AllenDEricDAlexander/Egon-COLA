package top.egon.cola.component.tianshu.admin.model.convertor;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiOptimizationRunBO;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiRunErrorCodeEnum;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiRunStatePersistenceEnum;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiRunTriggerPersistenceEnum;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiOptimizationRunResult;

/** 优化运行 BO 到视图的投影。 / Projection of an optimization-run BO into its view. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DdcAiOptimizationRunResultConverter
        extends BaseForwardConverter<DdcAiOptimizationRunBO, DdcAiOptimizationRunResult> {

    @Override
    @Mapping(target = "trigger", source = "trigger", qualifiedByName = "triggerValue")
    @Mapping(target = "state", source = "state", qualifiedByName = "runStateValue")
    @Mapping(target = "errorCode", source = "errorCode", qualifiedByName = "errorCodeValue")
    @Mapping(target = "createdAt", source = "createTime")
    @Mapping(target = "createdBy", source = "createUserId")
    DdcAiOptimizationRunResult toTarget(DdcAiOptimizationRunBO source);

    @Named("triggerValue")
    default String triggerValue(DdcAiRunTriggerPersistenceEnum value) {
        return value == null ? null : value.getValue();
    }

    @Named("runStateValue")
    default String runStateValue(DdcAiRunStatePersistenceEnum value) {
        return value == null ? null : value.getValue();
    }

    @Named("errorCodeValue")
    default String errorCodeValue(DdcAiRunErrorCodeEnum value) {
        return value == null ? null : value.getValue();
    }
}
