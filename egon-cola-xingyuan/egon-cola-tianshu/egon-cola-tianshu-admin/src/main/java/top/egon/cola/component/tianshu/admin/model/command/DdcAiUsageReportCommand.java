package top.egon.cola.component.tianshu.admin.model.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.tianshu.model.ai.DdcAiOutcomeEnum;

import java.time.Instant;

/**
 * RPC-003 单条使用上报命令；逐条校验，失败只拒绝该条。
 * / RPC-003 command for one usage report; validated per item so a failure rejects only that item.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder(toBuilder = true)
public class DdcAiUsageReportCommand {

    /** 下游幂等键；回执按原值返回，即使不合法。 / Downstream idempotency key, echoed in the receipt even when invalid. */
    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$")
    private String usageId;

    @NotBlank
    @Size(max = 128)
    private String namespaceCode;

    @NotBlank
    @Size(max = 128)
    private String resourceKey;

    @NotNull
    @Positive
    private Long resourceId;

    @NotNull
    @Positive
    private Long versionId;

    @NotNull
    @Positive
    private Long versionNo;

    @NotNull
    private DdcAiOutcomeEnum outcome;

    @Min(0)
    @Max(100)
    private Integer explicitScore;

    private String inputExcerpt;

    private String outputExcerpt;

    @PositiveOrZero
    private Integer latencyMs;

    @NotNull
    private Instant occurredAt;

    @Size(max = 64)
    private String traceId;
}
