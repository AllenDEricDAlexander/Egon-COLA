package top.egon.cola.component.tianshu.admin.model.convertor;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.component.tianshu.admin.model.bo.DdcNamespaceEnvAppBindingBO;
import top.egon.cola.component.tianshu.admin.model.po.DdcNamespaceEnvAppBindingPO;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DdcNamespaceEnvAppBindingConverter
        extends BaseConverter<DdcNamespaceEnvAppBindingPO, DdcNamespaceEnvAppBindingBO> {
}
