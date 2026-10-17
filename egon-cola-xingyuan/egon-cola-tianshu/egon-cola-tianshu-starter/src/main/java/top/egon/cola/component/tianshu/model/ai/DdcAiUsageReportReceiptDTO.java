package top.egon.cola.component.tianshu.model.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 批量上报回执，条目与请求同序同长。
 * / Usage-report batch receipt whose items keep the request order and length.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class DdcAiUsageReportReceiptDTO {

    @NotNull
    @Valid
    private List<ItemDTO> items;

    /**
     * 单条上报的受理结果；仅 REJECTED 携带原因码。
     * / Acceptance result of one report; only REJECTED carries a reason code.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class ItemDTO {

        @NotBlank
        private String usageId;

        @NotNull
        private DdcAiUsageReportStatusEnum status;

        @Size(max = 64)
        private String reason;
    }
}
