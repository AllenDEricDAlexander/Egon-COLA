package top.egon.cola.component.tianshu.admin.model.convertor;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiOptimizationPolicyBO;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiPolicyStatePersistenceEnum;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiOptimizationPolicyResult;

/**
 * 优化策略 BO 到视图的投影；评估快照由服务补充。
 * / Projection of an optimization-policy BO into its view; the service fills the evaluation snapshot.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DdcAiOptimizationPolicyResultConverter
        extends BaseForwardConverter<DdcAiOptimizationPolicyBO, DdcAiOptimizationPolicyResult> {

    @Override
    @Mapping(target = "persisted", expression = "java(source.getId() != null)")
    @Mapping(target = "revision", expression = "java(source.getVersion() == null ? 0L : source.getVersion())")
    @Mapping(target = "state", source = "state", qualifiedByName = "policyStateValue")
    @Mapping(target = "evaluation", ignore = true)
    DdcAiOptimizationPolicyResult toTarget(DdcAiOptimizationPolicyBO source);

    @Named("policyStateValue")
    default String policyStateValue(DdcAiPolicyStatePersistenceEnum value) {
        return value == null ? null : value.getValue();
    }
}
