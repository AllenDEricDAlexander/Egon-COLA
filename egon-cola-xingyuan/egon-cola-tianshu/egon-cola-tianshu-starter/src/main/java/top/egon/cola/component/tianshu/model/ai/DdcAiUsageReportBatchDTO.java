package top.egon.cola.component.tianshu.model.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 一次批量上报，最多 100 条。
 * / One usage-report batch holding at most 100 reports.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcAiUsageReportBatchDTO {

    @NotNull
    @Size(min = 1, max = 100)
    @Valid
    private List<DdcAiUsageReportDTO> reports;
}
