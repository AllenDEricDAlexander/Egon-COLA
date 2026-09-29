package top.egon.cola.component.rpc.tianshu.client.ai;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiNode;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiNodeType;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiPromptResult;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiResourceQuery;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiSkillResult;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeTypeEnum;
import top.egon.cola.component.tianshu.model.ai.DdcPromptResult;
import top.egon.cola.component.tianshu.model.ai.DdcSkillResult;

import java.util.List;

/** Maps the stable Prompt/Skill Port models to the published Direct RPC wire contract. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DdcAiProtoConverter extends BaseForwardConverter<DdcAiQueryDTO, AiResourceQuery> {

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "bizCode", source = "bizCode")
    @Mapping(target = "namespaceCode", source = "namespaceCode")
    @Mapping(target = "env", source = "env")
    @Mapping(target = "appCode", source = "appCode")
    @Mapping(target = "resourceKey", source = "resourceKey")
    @Mapping(target = "resourceId", expression = "java(Long.toString(source.resourceId()))")
    @Mapping(target = "versionNo", ignore = true)
    AiResourceQuery toTarget(DdcAiQueryDTO source);

    @BeanMapping(ignoreByDefault = true, nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    @Mapping(target = "resourceId", source = "resourceId", qualifiedByName = "positiveLong")
    @Mapping(target = "resourceKey", source = "resourceKey")
    @Mapping(target = "versionId", source = "versionId", qualifiedByName = "positiveLong")
    @Mapping(target = "versionNo", source = "versionNo", qualifiedByName = "positiveVersion")
    @Mapping(target = "contentSha256", source = "contentSha256")
    @Mapping(target = "template", source = "template")
    DdcPromptResult mapPromptResult(AiPromptResult source);

    @BeanMapping(ignoreByDefault = true, nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    @Mapping(target = "resourceId", source = "resourceId", qualifiedByName = "positiveString")
    @Mapping(target = "resourceKey", source = "resourceKey")
    @Mapping(target = "versionId", source = "versionId", qualifiedByName = "positiveString")
    @Mapping(target = "versionNo", source = "versionNo", qualifiedByName = "positiveVersion")
    @Mapping(target = "contentSha256", source = "contentSha256")
    @Mapping(target = "template", source = "template")
    AiPromptResult toProto(DdcPromptResult source);

    @BeanMapping(ignoreByDefault = true, nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    @Mapping(target = "resourceId", source = "resourceId", qualifiedByName = "positiveLong")
    @Mapping(target = "resourceKey", source = "resourceKey")
    @Mapping(target = "versionId", source = "versionId", qualifiedByName = "positiveLong")
    @Mapping(target = "versionNo", source = "versionNo", qualifiedByName = "positiveVersion")
    @Mapping(target = "contentSha256", source = "contentSha256")
    @Mapping(target = "files", source = "filesList")
    DdcSkillResult mapSkillResult(AiSkillResult source);

    @BeanMapping(ignoreByDefault = true, nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    @Mapping(target = "resourceId", source = "resourceId", qualifiedByName = "positiveString")
    @Mapping(target = "resourceKey", source = "resourceKey")
    @Mapping(target = "versionId", source = "versionId", qualifiedByName = "positiveString")
    @Mapping(target = "versionNo", source = "versionNo", qualifiedByName = "positiveVersion")
    @Mapping(target = "contentSha256", source = "contentSha256")
    @Mapping(target = "filesList", ignore = true)
    AiSkillResult mapSkillResultToProto(DdcSkillResult source);

    @BeanMapping(ignoreByDefault = true, nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    @Mapping(target = "path", source = "path")
    @Mapping(target = "nodeType", source = "nodeType", qualifiedByName = "nodeTypeFromProto")
    @Mapping(target = "content", source = ".", qualifiedByName = "nodeContent")
    DdcAiNodeDTO mapNode(AiNode source);

    @BeanMapping(ignoreByDefault = true, nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    @Mapping(target = "path", source = "path")
    @Mapping(target = "nodeType", source = "nodeType", qualifiedByName = "nodeTypeToProto")
    @Mapping(target = "content", source = "content")
    AiNode mapNode(DdcAiNodeDTO source);

    List<DdcAiNodeDTO> mapNodes(List<AiNode> source);

    default AiResourceQuery toQuery(DdcAiQueryDTO source) {
        if (source == null || !text(source.bizCode()) || !text(source.namespaceCode())
                || !text(source.env()) || !text(source.appCode()) || !text(source.resourceKey())
                || source.resourceId() == null || source.resourceId() <= 0
                || (source.versionNo() != null && source.versionNo() <= 0)) {
            throw new IllegalArgumentException("A complete positive-root AI query is required");
        }
        AiResourceQuery mapped = toTarget(source);
        return source.versionNo() == null
                ? mapped
                : mapped.toBuilder().setVersionNo(source.versionNo()).build();
    }

    default DdcPromptResult fromPromptResult(AiPromptResult source) {
        if (source == null || !text(source.getResourceKey()) || !validSha256(source.getContentSha256())) {
            throw new IllegalArgumentException("A complete published prompt response is required");
        }
        return mapPromptResult(source);
    }

    default DdcSkillResult fromSkillResult(AiSkillResult source) {
        if (source == null || !text(source.getResourceKey()) || !validSha256(source.getContentSha256())) {
            throw new IllegalArgumentException("A complete published skill response is required");
        }
        return mapSkillResult(source);
    }

    default AiSkillResult toProto(DdcSkillResult source) {
        if (source == null || source.getFiles() == null) {
            throw new IllegalArgumentException("A complete published skill result is required");
        }
        AiSkillResult.Builder result = mapSkillResultToProto(source).toBuilder();
        source.getFiles().forEach(node -> result.addFiles(toProto(node)));
        return result.build();
    }

    default AiNode toProto(DdcAiNodeDTO source) {
        if (source == null || source.getPath() == null || source.getNodeType() == null) {
            throw new IllegalArgumentException("Published skill node is invalid");
        }
        if (source.getNodeType() == DdcAiNodeTypeEnum.DIRECTORY && source.getContent() != null) {
            throw new IllegalArgumentException("Directory content must be absent");
        }
        if (source.getNodeType() == DdcAiNodeTypeEnum.FILE && source.getContent() == null) {
            throw new IllegalArgumentException("File content must be present");
        }
        return mapNode(source);
    }

    @Named("positiveLong")
    default long positiveLong(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,18}")) {
            throw new IllegalArgumentException("AI identifier must be a positive decimal Long");
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException("AI identifier exceeds Long range", invalid);
        }
    }

    @Named("positiveVersion")
    default long positiveVersion(long value) {
        if (value <= 0) throw new IllegalArgumentException("AI versionNo must be positive");
        return value;
    }

    @Named("positiveString")
    default String positiveString(Long value) {
        if (value == null || value <= 0) throw new IllegalArgumentException("AI identifier must be positive");
        return Long.toString(value);
    }

    @Named("nodeTypeFromProto")
    @ValueMapping(source = "DIRECTORY", target = "DIRECTORY")
    @ValueMapping(source = "FILE", target = "FILE")
    @ValueMapping(source = "AI_NODE_TYPE_UNSPECIFIED", target = MappingConstants.THROW_EXCEPTION)
    @ValueMapping(source = "UNRECOGNIZED", target = MappingConstants.THROW_EXCEPTION)
    DdcAiNodeTypeEnum nodeTypeFromProto(AiNodeType source);

    @Named("nodeTypeToProto")
    @ValueMapping(source = "DIRECTORY", target = "DIRECTORY")
    @ValueMapping(source = "FILE", target = "FILE")
    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.THROW_EXCEPTION)
    AiNodeType nodeTypeToProto(DdcAiNodeTypeEnum source);

    @Named("nodeContent")
    default String nodeContent(AiNode source) {
        if (source.getNodeType() == AiNodeType.DIRECTORY) {
            if (source.hasContent()) throw new IllegalArgumentException("Directory content must be absent");
            return null;
        }
        if (source.getNodeType() == AiNodeType.FILE && source.hasContent()) return source.getContent();
        throw new IllegalArgumentException("File content must be present");
    }

    private boolean text(String value) {
        return value != null && !value.isBlank();
    }

    private boolean validSha256(String value) {
        return value != null && value.matches("[0-9a-f]{64}");
    }
}
