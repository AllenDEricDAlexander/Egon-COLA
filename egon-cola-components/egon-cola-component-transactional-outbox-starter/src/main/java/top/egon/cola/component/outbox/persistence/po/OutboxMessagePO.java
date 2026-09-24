package top.egon.cola.component.outbox.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import top.egon.cola.component.common.core.pojo.BasePojo;
import top.egon.cola.component.common.mybatis.model.EgonModel;
import top.egon.cola.component.outbox.store.OutboxStatus;

import java.time.Instant;

/** Persistence model for the globally owned technical outbox message. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@TableName(value = "egon_cola_outbox_message", schema = "egon_outbox", autoResultMap = true)
public class OutboxMessagePO extends EgonModel<OutboxMessagePO> implements BasePojo {

    @TableField("message_id")
    @NotNull
    @Size(max = 64)
    private String messageId;

    @TableField("idempotency_key")
    @Size(max = 256)
    private String idempotencyKey;

    @TableField("message_fingerprint")
    @NotNull
    @Pattern(regexp = "[0-9a-f]{64}")
    private String messageFingerprint;

    @TableField("channel")
    @NotNull
    @Size(max = 64)
    private String channel;

    @TableField("destination")
    @NotNull
    @Size(max = 256)
    private String destination;

    @TableField("payload")
    @NotNull
    private String payload;

    @TableField("content_type")
    @NotNull
    @Size(max = 128)
    private String contentType;

    @TableField("schema_version")
    @Size(max = 32)
    private String schemaVersion;

    @TableField("headers_json")
    @NotNull
    private String headersJson;

    @TableField("trace_id")
    @Size(max = 128)
    private String traceId;

    @TableField("status")
    @NotNull
    private OutboxStatus status;

    @TableField("attempt_count")
    @Min(0)
    @Builder.Default
    private Integer attemptCount = 0;

    @TableField("max_attempts")
    @NotNull
    @Min(1)
    private Integer maxAttempts;

    @TableField("next_attempt_at")
    @NotNull
    private Instant nextAttemptAt;

    @TableField("locked_by")
    @Size(max = 128)
    private String lockedBy;

    @TableField("locked_until")
    private Instant lockedUntil;

    @TableField("last_error_code")
    @Size(max = 64)
    private String lastErrorCode;

    @TableField("last_error_message")
    private String lastErrorMessage;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;

    @TableField("completed_at")
    private Instant completedAt;
}
