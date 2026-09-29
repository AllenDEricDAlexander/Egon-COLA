package top.egon.cola.component.tianshu.admin.model.convertor;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiVersionBO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiVersionSummaryDTO;
import top.egon.cola.component.tianshu.admin.model.po.DdcAiVersionPO;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;

@Mapper(componentModel = "spring", uses = DdcAiEnumConverter.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DdcAiVersionConverter extends BaseConverter<DdcAiVersionPO, DdcAiVersionBO> {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "resourceId", source = "resourceId")
    @Mapping(target = "versionNo", source = "versionNo")
    @Mapping(target = "kind", source = "kind", qualifiedByName = "aiKindCode")
    @Mapping(target = "displayName", source = "displayName")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "contentSha256", source = "contentSha256")
    @Mapping(target = "contentBytes", source = "contentBytes")
    @Mapping(target = "nodeCount", source = "nodeCount")
    @Mapping(target = "sourceVersionId", source = "sourceVersionId")
    @Mapping(target = "changeNote", source = "changeNote")
    @Mapping(target = "createdAt", source = "createTime")
    @Mapping(target = "createdBy", source = "createUserId")
    DdcAiVersionSummaryDTO toVersionSummary(DdcAiVersionBO source);

    @Named("aiKindCode")
    default String aiKindCode(DdcAiKindEnum kind) {
        return kind == null ? null : kind.getCode();
    }
}
