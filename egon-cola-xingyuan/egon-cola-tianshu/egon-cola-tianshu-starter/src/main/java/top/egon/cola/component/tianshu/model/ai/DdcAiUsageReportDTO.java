package top.egon.cola.component.tianshu.model.ai;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
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

import java.time.Instant;

/**
 * 下游一次 Prompt/Skill 使用的结果上报；摘录在服务端按字节上限再次截断。
 * / One downstream Prompt or Skill usage report; excerpts are truncated again by byte limits on the server.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class DdcAiUsageReportDTO {

    /** 上报幂等键，在租户内永久唯一。 / Report idempotency key, permanently unique within the tenant. */
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
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long resourceId;

    /** 本次实际使用的不可变版本。 / Immutable version actually used by this call. */
    @NotNull
    @Positive
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long versionId;

    @NotNull
    @Positive
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long versionNo;

    @NotNull
    private DdcAiOutcomeEnum outcome;

    /** 下游显式评分；为空时由天枢自动评分。 / Explicit downstream score; Tianshu judges when absent. */
    @Min(0)
    @Max(100)
    private Integer explicitScore;

    @Size(max = 8192)
    private String inputExcerpt;

    @Size(max = 32768)
    private String outputExcerpt;

    @PositiveOrZero
    private Integer latencyMs;

    /** 使用发生的 UTC 瞬时。 / UTC instant at which the usage happened. */
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant occurredAt;

    @Size(max = 64)
    private String traceId;
}
