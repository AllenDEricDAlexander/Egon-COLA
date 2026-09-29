package top.egon.cola.component.tianshu.admin.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    private Long id;

    @NotNull
    @Positive
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long resourceId;

    @NotNull
    @Positive
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long versionNo;

    @NotBlank
    @Pattern(regexp = "^(PROMPT|SKILL)$")
    private String kind;

    @NotNull
    @Size(max = 128)
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
    private Long sourceVersionId;

    @NotNull
    @Size(max = 512)
    private String changeNote;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;
}
