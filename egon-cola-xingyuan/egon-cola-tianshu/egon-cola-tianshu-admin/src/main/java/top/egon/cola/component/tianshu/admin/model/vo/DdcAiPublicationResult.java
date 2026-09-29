package top.egon.cola.component.tianshu.admin.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
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

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcAiPublicationResult {

    @NotNull
    @Positive
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long publicationId;

    @NotNull
    @Positive
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long resourceId;

    @NotBlank
    @Pattern(regexp = "^(PUBLISH|ROLLBACK|PROMOTE)$")
    private String operation;

    @Positive
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long previousVersionId;

    @NotNull
    @Positive
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long targetVersionId;

    @NotNull
    @Positive
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long targetVersionNo;

    @Positive
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long sourceVersionId;

    @NotNull
    @Min(0)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long resourceRevision;

    @NotNull
    private Boolean targetCreated;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;
}
