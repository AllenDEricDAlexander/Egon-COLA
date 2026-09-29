package top.egon.cola.component.tianshu.admin.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcAiVersionSummaryDTO {

    @NotNull
    @Positive
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", pattern = "^[1-9][0-9]{0,18}$")
    private Long id;

    @NotNull
    @Positive
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", pattern = "^[1-9][0-9]{0,18}$")
    private Long resourceId;

    @NotNull
    @Positive
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", pattern = "^[1-9][0-9]{0,18}$")
    private Long versionNo;

    @NotBlank
    @Pattern(regexp = "^(PROMPT|SKILL)$")
    private String kind;

    @NotBlank
    @Size(min = 1, max = 128)
    @Schema(minLength = 1, maxLength = 128)
    private String displayName;

    @NotNull
    @Size(max = 2048)
    private String description;

    @NotNull
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String contentSha256;

    @NotNull
    @Min(0)
    private Long contentBytes;

    @NotNull
    @Min(0)
    @Max(256)
    private Integer nodeCount;

    @Positive
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", nullable = true, pattern = "^[1-9][0-9]{0,18}$")
    private Long sourceVersionId;

    @NotNull
    @Size(max = 512)
    private String changeNote;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;

    @NotBlank
    @Size(max = 128)
    @Pattern(regexp = "^[^\\p{Cntrl}]+$")
    @Schema(type = "string", pattern = "^[^\\p{Cntrl}]+$")
    private String createdBy;
}
