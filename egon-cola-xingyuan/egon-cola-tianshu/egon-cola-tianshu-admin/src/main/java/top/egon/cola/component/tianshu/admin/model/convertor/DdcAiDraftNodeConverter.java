package top.egon.cola.component.tianshu.admin.model.convertor;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiDraftNodeBO;
import top.egon.cola.component.tianshu.admin.model.po.DdcAiDraftNodePO;

@Mapper(componentModel = "spring", uses = DdcAiEnumConverter.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DdcAiDraftNodeConverter extends BaseConverter<DdcAiDraftNodePO, DdcAiDraftNodeBO> {
}
