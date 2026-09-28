package top.egon.cola.component.tianshu.admin.model.convertor;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.component.tianshu.admin.model.bo.DdcConfigItemBO;
import top.egon.cola.component.tianshu.admin.model.po.DdcConfigItemPO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcConfigVO;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DdcConfigItemConverter extends BaseConverter<DdcConfigItemPO, DdcConfigItemBO> {

    @Override
    @Mapping(target = "visibleNamespaces", ignore = true)
    DdcConfigItemBO toTarget(DdcConfigItemPO source);

    @Mapping(target = "id", source = "publicId")
    DdcConfigVO toConfigVO(DdcConfigItemBO source);
}
