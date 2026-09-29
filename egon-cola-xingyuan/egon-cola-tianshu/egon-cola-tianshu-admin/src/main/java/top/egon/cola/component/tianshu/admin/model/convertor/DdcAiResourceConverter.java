package top.egon.cola.component.tianshu.admin.model.convertor;

import org.mapstruct.Mapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiCreateCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiDraftCommand;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiResourceBO;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiKindPersistenceEnum;
import top.egon.cola.component.tianshu.admin.model.po.DdcAiResourcePO;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;

@Mapper(componentModel = "spring", uses = DdcAiEnumConverter.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DdcAiResourceConverter extends BaseConverter<DdcAiResourcePO, DdcAiResourceBO> {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "bizCode", source = "scope.bizCode")
    @Mapping(target = "namespaceCode", source = "scope.namespaceCode")
    @Mapping(target = "envCode", source = "scope.env")
    @Mapping(target = "appCode", source = "scope.appCode")
    @Mapping(target = "kind", source = "kind")
    @Mapping(target = "resourceKey", source = "resourceKey")
    @Mapping(target = "displayName", source = "displayName")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "draftPrompt", source = "draft.promptText")
    DdcAiResourceBO toBusiness(DdcAiCreateCommand command);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "displayName", source = "displayName")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "draftPrompt", source = "draft.promptText")
    DdcAiResourceBO toBusiness(DdcAiDraftCommand command);

    @Named("resourceKindToPersistence")
    DdcAiKindPersistenceEnum toPersistenceKind(DdcAiKindEnum kind);

    @Named("decimalStringToLong")
    default Long toInternalLong(String validatedDecimal) {
        return validatedDecimal == null ? null : Long.valueOf(validatedDecimal);
    }

    @Named("longToDecimalString")
    default String toExternalLong(Long internalId) {
        return internalId == null ? null : Long.toString(internalId);
    }
}
