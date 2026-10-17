package top.egon.cola.component.rpc.tianshu.client.ai;

import io.grpc.Status;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import top.egon.cola.component.rpc.tianshu.contract.DdcAiResourceRpc;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiOutcome;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiUsageReportBatch;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiUsageReportItemReceipt;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiUsageReportReceipt;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiUsageReportStatus;
import top.egon.cola.component.rpc.tianshu.mapping.DdcRpcStatusExceptionMapper;
import top.egon.cola.component.tianshu.error.DdcClientTransportException;
import top.egon.cola.component.tianshu.model.ai.DdcAiOutcomeEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiUsageReportBatchDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiUsageReportDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiUsageReportReceiptDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiUsageReportStatusEnum;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RpcDdcAiFeedbackClientTest {

    private final DdcAiResourceRpc rpc = mock(DdcAiResourceRpc.class);
    private final RpcDdcAiFeedbackClient client = new RpcDdcAiFeedbackClient(
            rpc, new DdcAiProtoConverterImpl(), new DdcRpcStatusExceptionMapper());

    @Test
    void reportMapsBatchWithOptionalFieldsAndKeepsReceiptOrder() {
        when(rpc.reportUsage(any())).thenReturn(AiUsageReportReceipt.newBuilder()
                .addItems(item("01JAX0M7Q9K2R5T8V1W4X7Y0ZB", AiUsageReportStatus.AI_USAGE_REPORT_ACCEPTED, null))
                .addItems(item("01JAX0M7Q9K2R5T8V1W4X7Y0ZC", AiUsageReportStatus.AI_USAGE_REPORT_REJECTED,
                        "CLOCK_SKEW"))
                .build());
        DdcAiUsageReportDTO scored = report("01JAX0M7Q9K2R5T8V1W4X7Y0ZB").toBuilder()
                .explicitScore(30).inputExcerpt("in").outputExcerpt("out").latencyMs(12).traceId("trace-1").build();

        DdcAiUsageReportReceiptDTO receipt = client.report(DdcAiUsageReportBatchDTO.builder()
                .reports(List.of(scored, report("01JAX0M7Q9K2R5T8V1W4X7Y0ZC"))).build());

        assertThat(receipt.getItems()).extracting(DdcAiUsageReportReceiptDTO.ItemDTO::getStatus)
                .containsExactly(DdcAiUsageReportStatusEnum.ACCEPTED, DdcAiUsageReportStatusEnum.REJECTED);
        assertThat(receipt.getItems().get(0).getReason()).isNull();
        assertThat(receipt.getItems().get(1).getReason()).isEqualTo("CLOCK_SKEW");
        ArgumentCaptor<AiUsageReportBatch> sent = ArgumentCaptor.forClass(AiUsageReportBatch.class);
        verify(rpc).reportUsage(sent.capture());
        assertThat(sent.getValue().getReportsCount()).isEqualTo(2);
        var first = sent.getValue().getReports(0);
        assertThat(first.getResourceId()).isEqualTo("11");
        assertThat(first.getVersionId()).isEqualTo("22");
        assertThat(first.getVersionNo()).isEqualTo(3L);
        assertThat(first.getOutcome()).isEqualTo(AiOutcome.AI_OUTCOME_FAILURE);
        assertThat(first.getOccurredAt()).isEqualTo("2026-10-17T04:58:12.345Z");
        assertThat(first.getExplicitScore()).isEqualTo(30);
        assertThat(first.getTraceId()).isEqualTo("trace-1");
        assertThat(sent.getValue().getReports(1).hasExplicitScore()).isFalse();
        assertThat(sent.getValue().getReports(1).hasInputExcerpt()).isFalse();
    }

    @Test
    void receiptLengthMismatchFailsClosed() {
        when(rpc.reportUsage(any())).thenReturn(AiUsageReportReceipt.getDefaultInstance());

        assertThatThrownBy(() -> client.report(DdcAiUsageReportBatchDTO.builder()
                .reports(List.of(report("01JAX0M7Q9K2R5T8V1W4X7Y0ZB"))).build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("AI_REPORT_RECEIPT_SIZE_MISMATCH");
    }

    @Test
    void unavailableTransportIsRestoredAsRetryable() {
        when(rpc.reportUsage(any())).thenThrow(Status.UNAVAILABLE.asRuntimeException());

        assertThatThrownBy(() -> client.report(DdcAiUsageReportBatchDTO.builder()
                .reports(List.of(report("01JAX0M7Q9K2R5T8V1W4X7Y0ZB"))).build()))
                .isInstanceOfSatisfying(DdcClientTransportException.class,
                        failure -> assertThat(failure.retryable()).isTrue());
    }

    private static AiUsageReportItemReceipt item(String usageId, AiUsageReportStatus status, String reason) {
        AiUsageReportItemReceipt.Builder builder = AiUsageReportItemReceipt.newBuilder()
                .setUsageId(usageId).setStatus(status);
        if (reason != null) {
            builder.setReason(reason);
        }
        return builder.build();
    }

    private static DdcAiUsageReportDTO report(String usageId) {
        return DdcAiUsageReportDTO.builder()
                .usageId(usageId).namespaceCode("prod-space").resourceKey("refund")
                .resourceId(11L).versionId(22L).versionNo(3L).outcome(DdcAiOutcomeEnum.FAILURE)
                .occurredAt(Instant.parse("2026-10-17T04:58:12.345Z"))
                .build();
    }
}
