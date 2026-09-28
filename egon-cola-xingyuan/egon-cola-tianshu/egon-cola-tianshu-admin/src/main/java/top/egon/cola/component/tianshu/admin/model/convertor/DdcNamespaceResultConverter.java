package top.egon.cola.component.tianshu.admin.model.convertor;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.component.tianshu.admin.model.bo.DdcNamespaceBO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcNamespaceResult;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DdcNamespaceResultConverter extends BaseForwardConverter<DdcNamespaceBO, DdcNamespaceResult> {
}
