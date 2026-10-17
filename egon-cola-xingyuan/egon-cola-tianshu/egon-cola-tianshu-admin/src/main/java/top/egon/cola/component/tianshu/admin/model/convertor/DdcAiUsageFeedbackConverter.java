package top.egon.cola.component.tianshu.admin.model.convertor;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiUsageFeedbackBO;
import top.egon.cola.component.tianshu.admin.model.po.DdcAiUsageFeedbackPO;

/** 使用样本 PO 与 BO 的双向转换。 / Bidirectional usage-sample PO and BO conversion. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DdcAiUsageFeedbackConverter extends BaseConverter<DdcAiUsageFeedbackPO, DdcAiUsageFeedbackBO> {
}
