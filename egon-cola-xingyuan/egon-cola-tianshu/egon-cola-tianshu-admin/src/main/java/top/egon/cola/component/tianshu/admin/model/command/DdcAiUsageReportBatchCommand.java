package top.egon.cola.component.tianshu.admin.model.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * RPC-003 批量上报命令；批级只校验长度，条目在服务内逐条校验。
 * / RPC-003 batch command; the batch only checks its size and items are validated one by one in the service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcAiUsageReportBatchCommand {

    @NotNull
    @Size(min = 1, max = 100)
    private List<@NotNull DdcAiUsageReportCommand> reports;
}
