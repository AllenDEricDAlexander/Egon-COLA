package top.egon.cola.component.tianshu.admin.model.convertor;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.component.tianshu.admin.model.bo.DdcBizBO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcBizResult;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DdcBizResultConverter extends BaseForwardConverter<DdcBizBO, DdcBizResult> {
    @Override
    @Mapping(target = "publicId", source = "publicId")
    DdcBizResult toTarget(DdcBizBO source);
}
