package top.egon.cola.component.tianshu.admin.model.convertor;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiResourceBO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiResourceSummaryDTO;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DdcAiResourceResultConverter
        extends BaseForwardConverter<DdcAiResourceBO, DdcAiResourceSummaryDTO> {

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "scope", expression = "java(new top.egon.cola.component.tianshu.model.ai.DdcAiScopeDTO(source.getBizCode(), source.getNamespaceCode(), source.getEnvCode(), source.getAppCode()))")
    @Mapping(target = "kind", source = "kind")
    @Mapping(target = "resourceKey", source = "resourceKey")
    @Mapping(target = "displayName", source = "displayName")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "revision", source = "version")
    @Mapping(target = "lastVersionNo", source = "lastVersionNo")
    @Mapping(target = "currentVersionId", source = "currentVersionId")
    @Mapping(target = "currentVersionNo", source = "currentVersionNo")
    @Mapping(target = "updatedAt", source = "updateTime")
    DdcAiResourceSummaryDTO toTarget(DdcAiResourceBO source);
}
