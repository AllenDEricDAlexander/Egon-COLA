package top.egon.cola.component.tianshu.admin.model.convertor;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiOptimizationPolicyBO;
import top.egon.cola.component.tianshu.admin.model.po.DdcAiOptimizationPolicyPO;

import java.time.Duration;

/**
 * 优化策略 PO 与 BO 的双向转换；PO 以秒存储窗口与冷却，BO 使用 Duration。
 * / Bidirectional optimization-policy conversion; the PO stores window and cooldown in seconds, the BO as Duration.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DdcAiOptimizationPolicyConverter
        extends BaseConverter<DdcAiOptimizationPolicyPO, DdcAiOptimizationPolicyBO> {

    @Override
    @Mapping(target = "evaluationWindow", source = "evaluationWindowSeconds", qualifiedByName = "secondsToDuration")
    @Mapping(target = "cooldown", source = "cooldownSeconds", qualifiedByName = "secondsToDuration")
    DdcAiOptimizationPolicyBO toTarget(DdcAiOptimizationPolicyPO source);

    @Override
    @InheritInverseConfiguration(name = "toTarget")
    @Mapping(target = "evaluationWindowSeconds", source = "evaluationWindow", qualifiedByName = "durationToSeconds")
    @Mapping(target = "cooldownSeconds", source = "cooldown", qualifiedByName = "durationToSeconds")
    DdcAiOptimizationPolicyPO toSource(DdcAiOptimizationPolicyBO target);

    @Named("secondsToDuration")
    default Duration secondsToDuration(Integer seconds) {
        return seconds == null ? null : Duration.ofSeconds(seconds);
    }

    @Named("durationToSeconds")
    default Integer durationToSeconds(Duration duration) {
        return duration == null ? null : Math.toIntExact(duration.toSeconds());
    }
}
