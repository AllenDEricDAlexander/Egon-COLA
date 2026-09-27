package top.egon.cola.component.tianshu.model.ai;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class DdcPromptResult {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long resourceId;

    private String resourceKey;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long versionId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long versionNo;

    private String contentSha256;

    private String template;
}
