package top.egon.cola.component.tianshu.admin.model.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.tianshu.model.config.DdcPublishMessage;

/** Validated internal command snapshot used to prepare and dispatch one YAML publication attempt. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcAtomicPublishCommand {

    @NotBlank
    @Size(max = 64)
    private String configId;

    @NotBlank
    @Size(max = 64)
    private String changeId;

    @NotBlank
    @Size(max = 128)
    private String bizCode;

    @NotBlank
    @Size(max = 128)
    private String env;

    @NotBlank
    @Size(max = 128)
    private String appCode;

    @NotBlank
    @Size(max = 255)
    private String resourceName;

    @PositiveOrZero
    private Long expectedPublishedVersion;

    @NotNull
    @Positive
    private Long targetVersion;

    private String content;

    @NotBlank
    @Size(max = 64)
    private String eventChecksum;

    @NotNull
    @Valid
    private DdcPublishMessage message;

    @NotNull
    @Positive
    private Long tenantId;

    @NotNull
    @PositiveOrZero
    private Integer attempt;

    @NotBlank
    @Size(max = 64)
    private String eventId;

    /** Preserves the former 11-field constructor signature for existing internal callers. */
    public DdcAtomicPublishCommand(
            String configId,
            String changeId,
            String bizCode,
            String env,
            String appCode,
            String resourceName,
            Long expectedPublishedVersion,
            Long targetVersion,
            String content,
            String eventChecksum,
            DdcPublishMessage message) {
        this(configId, changeId, bizCode, env, appCode, resourceName,
                expectedPublishedVersion, targetVersion, content, eventChecksum, message,
                null, null, null);
    }

    public String configId() { return configId; }
    public String changeId() { return changeId; }
    public String bizCode() { return bizCode; }
    public String env() { return env; }
    public String appCode() { return appCode; }
    public String resourceName() { return resourceName; }
    public Long expectedPublishedVersion() { return expectedPublishedVersion; }
    public Long targetVersion() { return targetVersion; }
    public String content() { return content; }
    public String eventChecksum() { return eventChecksum; }
    public DdcPublishMessage message() { return message; }
    public Long tenantId() { return tenantId; }
    public Integer attempt() { return attempt; }
    public String eventId() { return eventId; }
}
