package top.egon.cola.component.rpc.tianshu.client.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.rpc.tianshu.contract.DdcAiResourceRpc;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiUsageReportBatch;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiUsageReportReceipt;
import top.egon.cola.component.rpc.tianshu.mapping.DdcRpcStatusExceptionMapper;
import top.egon.cola.component.rpc.tianshu.security.DdcRpcOperation;
import top.egon.cola.component.tianshu.api.client.DdcAiFeedbackClient;
import top.egon.cola.component.tianshu.model.ai.DdcAiUsageReportBatchDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiUsageReportReceiptDTO;

/** Direct RPC adapter for the AI usage-feedback Port. */
@Validated
public final class RpcDdcAiFeedbackClient implements DdcAiFeedbackClient {

    private final DdcAiResourceRpc rpc;
    private final DdcAiProtoConverter converter;
    private final DdcRpcStatusExceptionMapper errors;

    public RpcDdcAiFeedbackClient(
            DdcAiResourceRpc rpc,
            DdcAiProtoConverter converter,
            DdcRpcStatusExceptionMapper errors) {
        this.rpc = rpc;
        this.converter = converter;
        this.errors = errors;
    }

    @Override
    public DdcAiUsageReportReceiptDTO report(@NotNull @Valid DdcAiUsageReportBatchDTO batch) {
        AiUsageReportBatch request = converter.toProto(batch);
        AiUsageReportReceipt response;
        try {
            response = rpc.reportUsage(request);
        } catch (RuntimeException failure) {
            throw errors.restore(failure, DdcRpcOperation.AI_REPORT);
        }
        DdcAiUsageReportReceiptDTO receipt = converter.fromReceipt(response);
        if (receipt.getItems().size() != batch.getReports().size()) {
            throw new IllegalStateException("AI_REPORT_RECEIPT_SIZE_MISMATCH");
        }
        return receipt;
    }
}
