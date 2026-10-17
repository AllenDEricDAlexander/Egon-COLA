package top.egon.cola.component.tianshu.admin.model.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.tianshu.model.ai.DdcAiUsageReportStatusEnum;

import java.util.List;

/** RPC-003 批量回执，条目与请求同序同长。 / RPC-003 batch receipt whose items keep the request order and length. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcAiUsageReportReceiptResult {

    @NotNull
    @Size(max = 100)
    @Valid
    private List<ItemResult> items;

    /** 单条受理结果。 / Acceptance result of one item. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class ItemResult {

        private String usageId;

        @NotNull
        private DdcAiUsageReportStatusEnum status;

        @Size(max = 64)
        private String reason;
    }
}
