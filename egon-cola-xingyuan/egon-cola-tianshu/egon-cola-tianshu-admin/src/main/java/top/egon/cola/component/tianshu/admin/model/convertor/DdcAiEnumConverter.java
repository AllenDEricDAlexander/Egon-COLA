package top.egon.cola.component.tianshu.admin.model.convertor;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ValueMapping;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiKindPersistenceEnum;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiNodePersistenceEnum;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiOutcomePersistenceEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeTypeEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiOutcomeEnum;

@Mapper(componentModel = "spring")
public interface DdcAiEnumConverter extends BaseConverter<DdcAiKindPersistenceEnum, DdcAiKindEnum> {

    @Override
    @ValueMapping(source = "PROMPT", target = "PROMPT")
    @ValueMapping(source = "SKILL", target = "SKILL")
    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.THROW_EXCEPTION)
    DdcAiKindEnum toTarget(DdcAiKindPersistenceEnum source);

    @Override
    @ValueMapping(source = "PROMPT", target = "PROMPT")
    @ValueMapping(source = "SKILL", target = "SKILL")
    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.THROW_EXCEPTION)
    DdcAiKindPersistenceEnum toSource(DdcAiKindEnum target);

    @ValueMapping(source = "DIRECTORY", target = "DIRECTORY")
    @ValueMapping(source = "FILE", target = "FILE")
    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.THROW_EXCEPTION)
    DdcAiNodeTypeEnum toSdkNode(DdcAiNodePersistenceEnum source);

    @ValueMapping(source = "DIRECTORY", target = "DIRECTORY")
    @ValueMapping(source = "FILE", target = "FILE")
    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.THROW_EXCEPTION)
    DdcAiNodePersistenceEnum toPersistenceNode(DdcAiNodeTypeEnum target);

    @ValueMapping(source = "SUCCESS", target = "SUCCESS")
    @ValueMapping(source = "FAILURE", target = "FAILURE")
    @ValueMapping(source = "UNKNOWN", target = "UNKNOWN")
    DdcAiOutcomePersistenceEnum toPersistenceOutcome(DdcAiOutcomeEnum source);

    @ValueMapping(source = "SUCCESS", target = "SUCCESS")
    @ValueMapping(source = "FAILURE", target = "FAILURE")
    @ValueMapping(source = "UNKNOWN", target = "UNKNOWN")
    DdcAiOutcomeEnum toSdkOutcome(DdcAiOutcomePersistenceEnum source);
}
