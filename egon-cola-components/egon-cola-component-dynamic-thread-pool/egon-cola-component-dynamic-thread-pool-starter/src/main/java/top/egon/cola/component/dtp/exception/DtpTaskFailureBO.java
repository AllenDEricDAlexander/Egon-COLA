package top.egon.cola.component.dtp.exception;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import top.egon.cola.component.dtp.domain.model.valobj.ExecutorKind;

import java.time.Instant;

/** 失败诊断输入；不保存任务参数、完整MDC或自定义ThreadLocal值。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DtpTaskFailureBO {
    public interface Report { }

    @NotBlank(groups = Report.class)
    private String failureId;
    private String appName;
    private String instanceId;
    private String executorName;
    private ExecutorKind executorKind;
    @NotBlank(groups = Report.class)
    private String source;
    private String traceId;
    private String requestId;
    private String threadName;
    private String methodName;
    @NotNull(groups = Report.class)
    private Instant observedAt;
    @NotNull(groups = Report.class)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Throwable cause;
    private boolean logEligible;
    @Min(value = 0, groups = Report.class)
    private long suppressedCount;
}
