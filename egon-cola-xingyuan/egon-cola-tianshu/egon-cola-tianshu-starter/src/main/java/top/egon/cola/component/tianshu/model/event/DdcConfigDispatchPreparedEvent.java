package top.egon.cola.component.tianshu.model.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.common.crypto.digest.Digests;
import top.egon.cola.component.tianshu.model.config.DdcPublishMessage;

import java.nio.charset.StandardCharsets;

/** Frozen tenant-scoped YAML publication intent stored in the transactional outbox. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcConfigDispatchPreparedEvent {

    private static final String EVENT_ID_DOMAIN = "tianshu.config-dispatch.event-id.v1";
    private static final String EVENT_CHECKSUM_DOMAIN = "tianshu.config-dispatch.event-checksum.v1";

    @NotBlank
    @Pattern(regexp = "^[a-f0-9]{64}$")
    private String eventId;

    @NotNull
    @Positive
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long tenantId;

    @NotBlank
    @Pattern(regexp = "^1$")
    private String schemaVersion;

    @NotNull
    @PositiveOrZero
    private Integer attempt;

    @NotBlank
    @Size(max = 64)
    private String configId;

    @PositiveOrZero
    private Long expectedPublishedVersion;

    @NotBlank
    @Pattern(regexp = "^[a-f0-9]{64}$")
    private String eventChecksum;

    @NotNull
    @Valid
    private DdcPublishMessage message;

    public static String eventId(Long tenantId, String changeId, Integer attempt) {
        if (tenantId == null || changeId == null || changeId.isBlank() || attempt == null || attempt < 0) {
            throw new IllegalArgumentException("tenant, changeId and attempt are required for event identity");
        }
        return Digests.sha256Hex(EVENT_ID_DOMAIN + field(Long.toString(tenantId))
                + field(changeId) + field(Integer.toString(attempt)));
    }

    public String calculateEventChecksum() {
        if (message == null) {
            throw new IllegalStateException("prepared event message is required for checksum");
        }
        return Digests.sha256Hex(EVENT_CHECKSUM_DOMAIN
                + field(eventId)
                + field(tenantId == null ? null : Long.toString(tenantId))
                + field(schemaVersion)
                + field(attempt == null ? null : Integer.toString(attempt))
                + field(configId)
                + field(expectedPublishedVersion == null ? null : Long.toString(expectedPublishedVersion))
                + field(message.getChecksum())
                + field(message.getPublishMode())
                + field(message.getOperator())
                + field(message.getTimestamp() == null ? null : Long.toString(message.getTimestamp())));
    }

    public boolean hasValidIdentityAndChecksum() {
        return message != null
                && tenantId != null
                && tenantId > 0
                && attempt != null
                && attempt >= 0
                && message.getChangeId() != null
                && !message.getChangeId().isBlank()
                && eventId != null
                && eventId.equals(eventId(tenantId, message.getChangeId(), attempt))
                && eventChecksum != null
                && eventChecksum.equals(calculateEventChecksum());
    }

    private static String field(String value) {
        if (value == null) {
            return "-1:";
        }
        return value.getBytes(StandardCharsets.UTF_8).length + ":" + value;
    }
}
