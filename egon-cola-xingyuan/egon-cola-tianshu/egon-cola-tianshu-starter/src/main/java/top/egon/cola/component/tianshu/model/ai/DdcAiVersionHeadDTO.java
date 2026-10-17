package top.egon.cola.component.tianshu.model.ai;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 资源当前发布版本头，用于下游对账而不传输正文；未发布时版本字段为空。
 * / Current published-version head used for downstream reconciliation without content; version fields are empty
 * when unpublished.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class DdcAiVersionHeadDTO {

    @NotNull
    @Positive
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long resourceId;

    @NotBlank
    private String resourceKey;

    @Positive
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long versionId;

    @Positive
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long versionNo;

    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String contentSha256;

    @NotNull
    @Min(0)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long resourceRevision;

    @NotNull
    private Boolean published;
}
