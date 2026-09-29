package top.egon.cola.component.tianshu.admin.model.convertor;

import org.mapstruct.Mapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiDraftNodeBO;
import top.egon.cola.component.tianshu.admin.model.po.DdcAiDraftNodePO;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeDTO;

@Mapper(componentModel = "spring", uses = DdcAiEnumConverter.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DdcAiDraftNodeConverter extends BaseConverter<DdcAiDraftNodePO, DdcAiDraftNodeBO> {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "path", source = "path")
    @Mapping(target = "nodeType", source = "nodeType")
    @Mapping(target = "content", source = "content")
    DdcAiDraftNodeBO toBusiness(DdcAiNodeDTO node);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "path", source = "path")
    @Mapping(target = "nodeType", source = "nodeType")
    @Mapping(target = "content", source = "content")
    DdcAiNodeDTO toDTO(DdcAiDraftNodeBO node);
}
