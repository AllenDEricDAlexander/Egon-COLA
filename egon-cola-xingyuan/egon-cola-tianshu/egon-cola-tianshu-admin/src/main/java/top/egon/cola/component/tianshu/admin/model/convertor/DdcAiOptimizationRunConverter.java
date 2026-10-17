package top.egon.cola.component.tianshu.admin.model.convertor;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiOptimizationRunBO;
import top.egon.cola.component.tianshu.admin.model.po.DdcAiOptimizationRunPO;

/** 优化运行 PO 与 BO 的双向转换。 / Bidirectional optimization-run PO and BO conversion. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DdcAiOptimizationRunConverter extends BaseConverter<DdcAiOptimizationRunPO, DdcAiOptimizationRunBO> {
}
