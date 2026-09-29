package top.egon.cola.component.tianshu.model.ai;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public static DdcAiNodeDTO of(
            @JsonProperty(value = "path", required = true) String path,
            @JsonProperty(value = "nodeType", required = true) DdcAiNodeTypeEnum nodeType,
            @JsonProperty(value = "content", required = true) String content) {
        return DdcAiNodeDTO.builder().path(path).nodeType(nodeType).content(content).build();
    }
}
