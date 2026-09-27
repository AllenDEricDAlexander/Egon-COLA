package top.egon.cola.component.tianshu.model.ai;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.tianshu.model.ai.validation.ValidAiNode;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@ValidAiNode
public class DdcAiNodeDTO {

    @NotNull
    @Size(max = 1024)
    private String path;

    @NotNull
    private DdcAiNodeTypeEnum nodeType;

    private String content;
}
