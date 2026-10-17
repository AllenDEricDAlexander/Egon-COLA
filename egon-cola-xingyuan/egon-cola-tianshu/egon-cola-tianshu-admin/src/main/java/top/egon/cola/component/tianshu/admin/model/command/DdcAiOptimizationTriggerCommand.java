package top.egon.cola.component.tianshu.admin.model.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** API-014 手动触发一次优化运行。 / API-014 manual trigger of one optimization run. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcAiOptimizationTriggerCommand {

    @NotNull
    @Size(max = 512)
    private String note;
}
