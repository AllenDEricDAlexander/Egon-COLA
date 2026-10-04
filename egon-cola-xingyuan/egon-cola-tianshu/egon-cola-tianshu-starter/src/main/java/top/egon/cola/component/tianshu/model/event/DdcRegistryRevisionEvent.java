package top.egon.cola.component.tianshu.model.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.common.crypto.digest.Digests;
import top.egon.cola.component.tianshu.model.registry.DdcRegistryEvent;

import java.nio.charset.StandardCharsets;

/** Frozen tenant-scoped registry revision notification stored in the transactional outbox. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcRegistryRevisionEvent {

    private static final String EVENT_ID_DOMAIN = "tianshu.registry-event.event-id.v1";

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
    @Valid
    private DdcRegistryEvent event;

    public static String eventId(Long tenantId, DdcRegistryEvent event) {
        if (tenantId == null || tenantId <= 0 || event == null || event.serviceKey() == null
                || event.serviceRevision() < 0 || event.catalogRevision() < 0) {
            throw new IllegalArgumentException("tenant and complete nonnegative registry revisions are required");
        }
        return Digests.sha256Hex(EVENT_ID_DOMAIN
                + field(Long.toString(tenantId))
                + field(event.serviceKey().canonicalValue())
                + field(Long.toString(event.serviceRevision()))
                + field(Long.toString(event.catalogRevision())));
    }

    public boolean hasValidIdentity() {
        return tenantId != null
                && tenantId > 0
                && "1".equals(schemaVersion)
                && event != null
                && event.serviceKey() != null
                && event.serviceRevision() >= 0
                && event.catalogRevision() >= 0
                && eventId != null
                && eventId.equals(eventId(tenantId, event));
    }

    private static String field(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length + ":" + value;
    }
}
