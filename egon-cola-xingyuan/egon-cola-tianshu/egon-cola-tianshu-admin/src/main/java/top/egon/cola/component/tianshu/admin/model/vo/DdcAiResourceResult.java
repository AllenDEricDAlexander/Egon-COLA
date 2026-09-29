package top.egon.cola.component.tianshu.admin.model.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiResourceSummaryDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiContentDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcAiResourceResult {

    @NotNull
    @Valid
    private DdcAiResourceSummaryDTO resource;

    @NotNull
    @Valid
    private DdcAiContentDTO draft;
}
