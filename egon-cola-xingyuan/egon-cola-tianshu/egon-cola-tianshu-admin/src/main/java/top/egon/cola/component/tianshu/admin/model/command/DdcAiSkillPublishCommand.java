package top.egon.cola.component.tianshu.admin.model.command;

import jakarta.validation.constraints.DecimalMax;
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

/**
 * RPC-006：以客户端请求 ID 幂等发布已上传的 Skill 草稿。
 * / RPC-006: publishes an uploaded Skill draft idempotently by the client request ID.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcAiSkillPublishCommand {

    @NotBlank
    @Size(max = 128)
    private String namespaceCode;

    @NotBlank
    @Size(max = 128)
    private String resourceKey;

    @NotNull
    @Positive
    private Long resourceId;

    @NotBlank
    @Size(max = 19)
    @Pattern(regexp = "^(0|[1-9][0-9]*)$")
    @DecimalMax("9223372036854775807")
    private String expectedRevision;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$")
    private String requestId;

    @NotNull
    @Size(max = 512)
    private String changeNote;
}
