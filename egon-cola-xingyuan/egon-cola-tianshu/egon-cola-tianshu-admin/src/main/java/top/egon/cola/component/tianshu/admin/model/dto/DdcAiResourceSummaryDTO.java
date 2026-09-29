package top.egon.cola.component.tianshu.admin.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    private String id;

    @NotNull
    @Valid
    private DdcAiScopeDTO scope;

    @NotNull
    private DdcAiKindEnum kind;

    @NotNull
    @Size(max = 128)
    private String resourceKey;

    @NotNull
    @Size(max = 128)
    private String displayName;

    @NotNull
    @Size(max = 2048)
    private String description;

    @NotNull
    private String revision;

    @NotNull
    private String lastVersionNo;

    private String currentVersionId;

    private String currentVersionNo;

    @NotNull
    private Instant updatedAt;
}
