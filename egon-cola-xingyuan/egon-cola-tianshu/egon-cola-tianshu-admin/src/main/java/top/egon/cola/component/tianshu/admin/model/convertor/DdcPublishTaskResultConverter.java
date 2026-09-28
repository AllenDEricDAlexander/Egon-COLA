package top.egon.cola.component.tianshu.admin.model.convertor;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.component.tianshu.admin.model.bo.DdcPublishTaskBO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPublishTaskResult;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DdcPublishTaskResultConverter extends BaseForwardConverter<DdcPublishTaskBO, DdcPublishTaskResult> {

    @Override
    @Mapping(target = "publicId", source = "publicId")
    DdcPublishTaskResult toTarget(DdcPublishTaskBO source);
}
