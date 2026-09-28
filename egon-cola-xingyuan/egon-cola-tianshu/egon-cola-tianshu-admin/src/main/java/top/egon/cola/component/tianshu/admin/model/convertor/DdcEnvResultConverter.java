package top.egon.cola.component.tianshu.admin.model.convertor;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.component.tianshu.admin.model.bo.DdcEnvBO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcEnvResult;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DdcEnvResultConverter extends BaseForwardConverter<DdcEnvBO, DdcEnvResult> {
}
