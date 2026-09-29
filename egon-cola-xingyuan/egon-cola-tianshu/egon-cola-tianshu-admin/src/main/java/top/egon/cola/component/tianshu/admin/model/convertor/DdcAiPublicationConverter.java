package top.egon.cola.component.tianshu.admin.model.convertor;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiPublicationBO;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiPublicationOperationEnum;
import top.egon.cola.component.tianshu.admin.model.po.DdcAiPublicationPO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiPublicationResult;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DdcAiPublicationConverter extends BaseConverter<DdcAiPublicationPO, DdcAiPublicationBO> {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "publicationId", source = "id")
    @Mapping(target = "resourceId", source = "resourceId")
    @Mapping(target = "operation", source = "operation", qualifiedByName = "publicationOperationValue")
    @Mapping(target = "previousVersionId", source = "previousVersionId")
    @Mapping(target = "targetVersionId", source = "targetVersionId")
    @Mapping(target = "targetVersionNo", source = "targetVersionNo")
    @Mapping(target = "sourceVersionId", source = "sourceVersionId")
    @Mapping(target = "resourceRevision", source = "resultRevision")
    @Mapping(target = "targetCreated", source = "targetCreated")
    @Mapping(target = "createdAt", source = "createTime")
    DdcAiPublicationResult toResult(DdcAiPublicationBO source);

    @Named("publicationOperationValue")
    default String publicationOperationValue(DdcAiPublicationOperationEnum operation) {
        return operation == null ? null : operation.getValue();
    }
}
