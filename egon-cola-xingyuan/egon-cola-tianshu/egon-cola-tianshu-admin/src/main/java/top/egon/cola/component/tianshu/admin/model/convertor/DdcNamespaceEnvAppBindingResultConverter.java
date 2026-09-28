package top.egon.cola.component.tianshu.admin.model.convertor;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.component.tianshu.admin.model.bo.DdcNamespaceEnvAppBindingBO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcNamespaceEnvAppBindingResult;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DdcNamespaceEnvAppBindingResultConverter
        extends BaseForwardConverter<DdcNamespaceEnvAppBindingBO, DdcNamespaceEnvAppBindingResult> {

    @Override
    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "namespaceId", source = "namespacePublicId")
    @Mapping(target = "env", source = "envCode")
    @Mapping(target = "appId", source = "appPublicId")
    DdcNamespaceEnvAppBindingResult toTarget(DdcNamespaceEnvAppBindingBO source);
}
