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
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiOutcome;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiPromptResult;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiPublicationReceipt;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiResourceQuery;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiRevisionResult;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiSkillDraftUpload;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiSkillPublishRequest;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiSkillResult;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiUsageReport;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiUsageReportBatch;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiUsageReportItemReceipt;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiUsageReportReceipt;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiUsageReportStatus;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiVersionHead;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeTypeEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiOutcomeEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiPublicationReceiptDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiRevisionDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiSkillDraftUploadDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiSkillPublishDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiUsageReportBatchDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiUsageReportDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiUsageReportReceiptDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiUsageReportStatusEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiVersionHeadDTO;
import top.egon.cola.component.tianshu.model.ai.DdcPromptResult;
import top.egon.cola.component.tianshu.model.ai.DdcSkillResult;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Maps the stable Prompt/Skill, usage-feedback and Skill-sync Port models to the published Direct RPC wire contract. */
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

    default AiUsageReportBatch toProto(DdcAiUsageReportBatchDTO source) {
        if (source == null || source.getReports() == null || source.getReports().isEmpty()) {
            throw new IllegalArgumentException("A non-empty AI usage report batch is required");
        }
        AiUsageReportBatch.Builder batch = AiUsageReportBatch.newBuilder();
        source.getReports().forEach(report -> batch.addReports(toProto(report)));
        return batch.build();
    }

    default AiUsageReport toProto(DdcAiUsageReportDTO source) {
        if (source == null || !text(source.getUsageId()) || source.getOutcome() == null
                || source.getOccurredAt() == null) {
            throw new IllegalArgumentException("A complete AI usage report is required");
        }
        AiUsageReport.Builder report = AiUsageReport.newBuilder()
                .setUsageId(source.getUsageId())
                .setNamespaceCode(source.getNamespaceCode())
                .setResourceKey(source.getResourceKey())
                .setResourceId(positiveString(source.getResourceId()))
                .setVersionId(positiveString(source.getVersionId()))
                .setVersionNo(positiveVersion(source.getVersionNo() == null ? 0L : source.getVersionNo()))
                .setOutcome(outcomeToProto(source.getOutcome()))
                .setOccurredAt(instantToRfc3339(source.getOccurredAt()));
        if (source.getExplicitScore() != null) report.setExplicitScore(source.getExplicitScore());
        if (source.getInputExcerpt() != null) report.setInputExcerpt(source.getInputExcerpt());
        if (source.getOutputExcerpt() != null) report.setOutputExcerpt(source.getOutputExcerpt());
        if (source.getLatencyMs() != null) report.setLatencyMs(source.getLatencyMs());
        if (source.getTraceId() != null) report.setTraceId(source.getTraceId());
        return report.build();
    }

    default DdcAiUsageReportReceiptDTO fromReceipt(AiUsageReportReceipt source) {
        if (source == null) {
            throw new IllegalArgumentException("An AI usage report receipt is required");
        }
        List<DdcAiUsageReportReceiptDTO.ItemDTO> items = source.getItemsList().stream()
                .map(this::fromReceiptItem)
                .toList();
        return DdcAiUsageReportReceiptDTO.builder().items(items).build();
    }

    default DdcAiUsageReportReceiptDTO.ItemDTO fromReceiptItem(AiUsageReportItemReceipt source) {
        if (source == null || !text(source.getUsageId())) {
            throw new IllegalArgumentException("A complete AI usage receipt item is required");
        }
        return DdcAiUsageReportReceiptDTO.ItemDTO.builder()
                .usageId(source.getUsageId())
                .status(statusFromProto(source.getStatus()))
                .reason(source.hasReason() ? source.getReason() : null)
                .build();
    }

    default DdcAiVersionHeadDTO fromVersionHead(AiVersionHead source) {
        if (source == null || !text(source.getResourceKey())) {
            throw new IllegalArgumentException("A complete AI version head is required");
        }
        if (source.getPublished() && (!source.hasVersionNo() || !source.hasVersionId()
                || !source.hasContentSha256() || !validSha256(source.getContentSha256()))) {
            throw new IllegalArgumentException("A published AI version head requires version and digest");
        }
        return DdcAiVersionHeadDTO.builder()
                .resourceId(positiveLong(source.getResourceId()))
                .resourceKey(source.getResourceKey())
                .versionId(source.hasVersionId() ? positiveLong(source.getVersionId()) : null)
                .versionNo(source.hasVersionNo() ? positiveVersion(source.getVersionNo()) : null)
                .contentSha256(source.hasContentSha256() ? source.getContentSha256() : null)
                .resourceRevision(nonNegativeLong(source.getResourceRevision()))
                .published(source.getPublished())
                .build();
    }

    default AiSkillDraftUpload toProto(DdcAiSkillDraftUploadDTO source) {
        if (source == null || source.getFiles() == null || source.getFiles().isEmpty()
                || source.getExpectedRevision() == null || source.getExpectedRevision() < 0) {
            throw new IllegalArgumentException("A complete Skill draft upload is required");
        }
        AiSkillDraftUpload.Builder upload = AiSkillDraftUpload.newBuilder()
                .setNamespaceCode(source.getNamespaceCode())
                .setResourceKey(source.getResourceKey())
                .setResourceId(positiveString(source.getResourceId()))
                .setExpectedRevision(Long.toString(source.getExpectedRevision()))
                .setDisplayName(source.getDisplayName())
                .setDescription(source.getDescription());
        source.getFiles().forEach(node -> upload.addFiles(toProto(node)));
        return upload.build();
    }

    default DdcAiRevisionDTO fromRevision(AiRevisionResult source) {
        if (source == null || !validSha256(source.getDraftSha256())) {
            throw new IllegalArgumentException("A complete AI revision result is required");
        }
        return DdcAiRevisionDTO.builder()
                .resourceId(positiveLong(source.getResourceId()))
                .resourceRevision(nonNegativeLong(source.getResourceRevision()))
                .draftSha256(source.getDraftSha256())
                .draftMatchesCurrent(source.getDraftMatchesCurrent())
                .build();
    }

    default AiSkillPublishRequest toProto(DdcAiSkillPublishDTO source) {
        if (source == null || !text(source.getRequestId()) || source.getExpectedRevision() == null
                || source.getExpectedRevision() < 0 || source.getChangeNote() == null) {
            throw new IllegalArgumentException("A complete Skill publish request is required");
        }
        return AiSkillPublishRequest.newBuilder()
                .setNamespaceCode(source.getNamespaceCode())
                .setResourceKey(source.getResourceKey())
                .setResourceId(positiveString(source.getResourceId()))
                .setExpectedRevision(Long.toString(source.getExpectedRevision()))
                .setRequestId(source.getRequestId())
                .setChangeNote(source.getChangeNote())
                .build();
    }

    default DdcAiPublicationReceiptDTO fromPublicationReceipt(AiPublicationReceipt source) {
        if (source == null || !validSha256(source.getContentSha256())) {
            throw new IllegalArgumentException("A complete AI publication receipt is required");
        }
        return DdcAiPublicationReceiptDTO.builder()
                .publicationId(positiveLong(source.getPublicationId()))
                .resourceId(positiveLong(source.getResourceId()))
                .targetVersionId(positiveLong(source.getTargetVersionId()))
                .targetVersionNo(positiveVersion(source.getTargetVersionNo()))
                .resourceRevision(nonNegativeLong(source.getResourceRevision()))
                .contentSha256(source.getContentSha256())
                .build();
    }

    @Named("outcomeToProto")
    @ValueMapping(source = "SUCCESS", target = "AI_OUTCOME_SUCCESS")
    @ValueMapping(source = "FAILURE", target = "AI_OUTCOME_FAILURE")
    @ValueMapping(source = "UNKNOWN", target = "AI_OUTCOME_UNKNOWN")
    AiOutcome outcomeToProto(DdcAiOutcomeEnum source);

    @Named("outcomeFromProto")
    @ValueMapping(source = "AI_OUTCOME_SUCCESS", target = "SUCCESS")
    @ValueMapping(source = "AI_OUTCOME_FAILURE", target = "FAILURE")
    @ValueMapping(source = "AI_OUTCOME_UNKNOWN", target = "UNKNOWN")
    @ValueMapping(source = "AI_OUTCOME_UNSPECIFIED", target = MappingConstants.THROW_EXCEPTION)
    @ValueMapping(source = "UNRECOGNIZED", target = MappingConstants.THROW_EXCEPTION)
    DdcAiOutcomeEnum outcomeFromProto(AiOutcome source);

    @Named("statusFromProto")
    @ValueMapping(source = "AI_USAGE_REPORT_ACCEPTED", target = "ACCEPTED")
    @ValueMapping(source = "AI_USAGE_REPORT_ACCEPTED_DUPLICATE", target = "ACCEPTED_DUPLICATE")
    @ValueMapping(source = "AI_USAGE_REPORT_REJECTED", target = "REJECTED")
    @ValueMapping(source = "AI_USAGE_REPORT_STATUS_UNSPECIFIED", target = MappingConstants.THROW_EXCEPTION)
    @ValueMapping(source = "UNRECOGNIZED", target = MappingConstants.THROW_EXCEPTION)
    DdcAiUsageReportStatusEnum statusFromProto(AiUsageReportStatus source);

    @Named("statusToProto")
    @ValueMapping(source = "ACCEPTED", target = "AI_USAGE_REPORT_ACCEPTED")
    @ValueMapping(source = "ACCEPTED_DUPLICATE", target = "AI_USAGE_REPORT_ACCEPTED_DUPLICATE")
    @ValueMapping(source = "REJECTED", target = "AI_USAGE_REPORT_REJECTED")
    AiUsageReportStatus statusToProto(DdcAiUsageReportStatusEnum source);

    @Named("instantToRfc3339")
    default String instantToRfc3339(Instant value) {
        if (value == null) throw new IllegalArgumentException("AI instant is required");
        return DateTimeFormatter.ISO_INSTANT.format(value);
    }

    @Named("rfc3339ToInstant")
    default Instant rfc3339ToInstant(String value) {
        if (!text(value)) throw new IllegalArgumentException("AI instant is required");
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(value, Instant::from);
    }

    @Named("nonNegativeLong")
    default long nonNegativeLong(String value) {
        if (value == null || !value.matches("0|[1-9][0-9]{0,18}")) {
            throw new IllegalArgumentException("AI revision must be a non-negative decimal Long");
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException("AI revision exceeds Long range", invalid);
        }
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
