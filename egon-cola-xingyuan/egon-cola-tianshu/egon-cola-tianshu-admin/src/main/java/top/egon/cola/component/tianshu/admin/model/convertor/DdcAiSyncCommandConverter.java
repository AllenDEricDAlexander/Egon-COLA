package top.egon.cola.component.tianshu.admin.model.convertor;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiOutcome;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiUsageReport;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiUsageReportBatch;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiUsageReportItemReceipt;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiUsageReportReceipt;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiUsageReportStatus;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiUsageReportBatchCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiUsageReportCommand;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiUsageReportReceiptResult;
import top.egon.cola.component.tianshu.model.ai.DdcAiOutcomeEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiUsageReportStatusEnum;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * AI 同步与上报 RPC 消息到 Admin 命令/结果的转换；不合法的标量转为 null，由逐条校验拒绝。
 * / Converts AI sync and report RPC messages to Admin commands and results; invalid scalars become null so per-item
 * validation can reject them.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DdcAiSyncCommandConverter extends BaseForwardConverter<AiUsageReportBatch, DdcAiUsageReportBatchCommand> {

    @Override
    default DdcAiUsageReportBatchCommand toTarget(AiUsageReportBatch source) {
        if (source == null) throw new IllegalArgumentException("AI usage report batch is required");
        return DdcAiUsageReportBatchCommand.builder()
                .reports(source.getReportsList().stream().map(this::toCommand).toList())
                .build();
    }

    default DdcAiUsageReportCommand toCommand(AiUsageReport source) {
        return DdcAiUsageReportCommand.builder()
                .usageId(source.getUsageId())
                .namespaceCode(source.getNamespaceCode())
                .resourceKey(source.getResourceKey())
                .resourceId(decimalOrNull(source.getResourceId()))
                .versionId(decimalOrNull(source.getVersionId()))
                .versionNo(source.getVersionNo() > 0 ? source.getVersionNo() : null)
                .outcome(outcomeOrNull(source.getOutcome()))
                .explicitScore(source.hasExplicitScore() ? source.getExplicitScore() : null)
                .inputExcerpt(source.hasInputExcerpt() ? source.getInputExcerpt() : null)
                .outputExcerpt(source.hasOutputExcerpt() ? source.getOutputExcerpt() : null)
                .latencyMs(source.hasLatencyMs() ? source.getLatencyMs() : null)
                .occurredAt(instantOrNull(source.getOccurredAt()))
                .traceId(source.hasTraceId() ? source.getTraceId() : null)
                .build();
    }

    default AiUsageReportReceipt toProto(DdcAiUsageReportReceiptResult source) {
        AiUsageReportReceipt.Builder receipt = AiUsageReportReceipt.newBuilder();
        for (DdcAiUsageReportReceiptResult.ItemResult item : source.getItems()) {
            AiUsageReportItemReceipt.Builder builder = AiUsageReportItemReceipt.newBuilder()
                    .setUsageId(item.getUsageId() == null ? "" : item.getUsageId())
                    .setStatus(statusToProto(item.getStatus()));
            if (item.getReason() != null) builder.setReason(item.getReason());
            receipt.addItems(builder);
        }
        return receipt.build();
    }

    @Named("outcomeFromProto")
    @ValueMapping(source = "AI_OUTCOME_SUCCESS", target = "SUCCESS")
    @ValueMapping(source = "AI_OUTCOME_FAILURE", target = "FAILURE")
    @ValueMapping(source = "AI_OUTCOME_UNKNOWN", target = "UNKNOWN")
    @ValueMapping(source = "AI_OUTCOME_UNSPECIFIED", target = MappingConstants.NULL)
    @ValueMapping(source = "UNRECOGNIZED", target = MappingConstants.NULL)
    DdcAiOutcomeEnum outcomeOrNull(AiOutcome source);

    @Named("statusToProto")
    @ValueMapping(source = "ACCEPTED", target = "AI_USAGE_REPORT_ACCEPTED")
    @ValueMapping(source = "ACCEPTED_DUPLICATE", target = "AI_USAGE_REPORT_ACCEPTED_DUPLICATE")
    @ValueMapping(source = "REJECTED", target = "AI_USAGE_REPORT_REJECTED")
    AiUsageReportStatus statusToProto(DdcAiUsageReportStatusEnum source);

    @Named("decimalOrNull")
    default Long decimalOrNull(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,18}")) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException overflow) {
            return null;
        }
    }

    @Named("instantOrNull")
    default Instant instantOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(value, Instant::from);
        } catch (DateTimeParseException invalid) {
            return null;
        }
    }
}
