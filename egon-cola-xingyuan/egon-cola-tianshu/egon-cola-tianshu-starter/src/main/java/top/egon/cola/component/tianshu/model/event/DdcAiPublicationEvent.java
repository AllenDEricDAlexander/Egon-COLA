package top.egon.cola.component.tianshu.model.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
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

/**
 * 已提交的 AI Prompt/Skill 发布事实，经事务 Outbox 投递到租户 AI Topic；不携带正文。
 * / Committed AI Prompt or Skill publication fact delivered through the transactional outbox to the tenant AI
 * topic; it carries no content.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class DdcAiPublicationEvent {

    /** 由发布回执派生的稳定事件标识。 / Stable event identity derived from the publication receipt. */
    @NotBlank
    @Pattern(regexp = "^ai-pub-[1-9][0-9]{0,18}$")
    private String eventId;

    @NotNull
    @Positive
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long tenantId;

    @NotNull
    @Positive
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long resourceId;

    @NotBlank
    @Pattern(regexp = "^(PROMPT|SKILL)$")
    private String kind;

    @NotBlank
    @Size(max = 128)
    private String bizCode;

    @NotBlank
    @Size(max = 128)
    private String namespaceCode;

    @NotBlank
    @Size(max = 32)
    private String env;

    @NotBlank
    @Size(max = 128)
    private String appCode;

    @NotBlank
    @Size(max = 128)
    private String resourceKey;

    @NotBlank
    @Pattern(regexp = "^(PUBLISH|ROLLBACK|PROMOTE)$")
    private String operation;

    @NotNull
    @Positive
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long versionId;

    @NotNull
    @Positive
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long versionNo;

    @NotBlank
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String contentSha256;

    @NotNull
    @Min(0)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long resourceRevision;

    @NotBlank
    @Size(max = 512)
    private String actor;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant occurredAt;

    /** 判断事件是否携带可路由的完整身份。 / Returns whether the event carries a complete routable identity. */
    public boolean hasValidIdentity() {
        return eventId != null
                && eventId.matches("^ai-pub-[1-9][0-9]{0,18}$")
                && tenantId != null
                && tenantId > 0
                && resourceId != null
                && resourceId > 0
                && versionId != null
                && versionId > 0
                && versionNo != null
                && versionNo > 0;
    }
}
