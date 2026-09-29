package top.egon.cola.component.tianshu.admin.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiScopeDTO;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcAiResourceSummaryDTO {

    @NotNull
    @Pattern(regexp = "^[1-9][0-9]{0,18}$")
    @Schema(type = "string", pattern = "^[1-9][0-9]{0,18}$")
    private String id;

    @NotNull
    @Valid
    private DdcAiScopeDTO scope;

    @NotNull
    private DdcAiKindEnum kind;

    @NotBlank
    @Size(max = 128)
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$")
    private String resourceKey;

    @NotBlank
    @Size(min = 1, max = 128)
    @Schema(minLength = 1, maxLength = 128)
    private String displayName;

    @NotNull
    @Size(max = 2048)
    private String description;

    @NotNull
    @Pattern(regexp = "^(0|[1-9][0-9]{0,18})$")
    @Schema(type = "string", pattern = "^(0|[1-9][0-9]{0,18})$")
    private String revision;

    @NotNull
    @Pattern(regexp = "^(0|[1-9][0-9]{0,18})$")
    @Schema(type = "string", pattern = "^(0|[1-9][0-9]{0,18})$")
    private String lastVersionNo;

    @Pattern(regexp = "^[1-9][0-9]{0,18}$")
    @Schema(type = "string", nullable = true, pattern = "^[1-9][0-9]{0,18}$")
    private String currentVersionId;

    @Pattern(regexp = "^[1-9][0-9]{0,18}$")
    @Schema(type = "string", nullable = true, pattern = "^[1-9][0-9]{0,18}$")
    private String currentVersionNo;

    @NotNull
    private Instant updatedAt;
}
