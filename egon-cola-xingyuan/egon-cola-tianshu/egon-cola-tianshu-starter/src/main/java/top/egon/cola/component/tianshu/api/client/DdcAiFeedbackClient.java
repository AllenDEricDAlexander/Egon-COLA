package top.egon.cola.component.tianshu.api.client;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import top.egon.cola.component.tianshu.model.ai.DdcAiUsageReportBatchDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiUsageReportReceiptDTO;

/**
 * 下游向天枢批量上报 Prompt/Skill 使用结果的端口。
 * / Port through which downstream applications report Prompt and Skill usage outcomes to Tianshu in batches.
 */
public interface DdcAiFeedbackClient {

    /** 批量上报；回执条目与请求同序同长。 / Reports a batch; receipt items keep the request order and length. */
    DdcAiUsageReportReceiptDTO report(@NotNull @Valid DdcAiUsageReportBatchDTO batch);
}
