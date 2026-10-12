package top.egon.cola.component.yuheng.edge.service.domain.bo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import top.egon.cola.component.yuheng.edge.service.domain.enums.EdgeSessionBindingStateEnum;

/** Encrypted authoritative lifecycle state; never a browser identity payload. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder(toBuilder = true)
public class EdgeSessionBindingBO {
    @Min(2) @Max(2) @Builder.Default private int schemaVersion = 2;
    @NotBlank @Pattern(regexp = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    @ToString.Exclude private String bindingId;
    @Positive private long generation;
    @Positive private long fencingToken;
    @ToString.Exclude private URI issuer;
    @Pattern(regexp = "[1-9][0-9]{0,18}") @DecimalMax("9223372036854775807") @ToString.Exclude private String tenantId;
    @Size(min = 1, max = 256) @ToString.Exclude private String subject;
    @NotNull private Instant originalCreatedAt;
    @NotNull private Instant absoluteExpiresAt;
    private Instant closedAt;
    @NotNull @Builder.Default private EdgeSessionBindingStateEnum state = EdgeSessionBindingStateEnum.NEW;

    @JsonIgnore
    @AssertTrue(message = "Edge lifecycle state, canonical identity and original absolute deadline must agree")
    public boolean isStateValid() {
        if (state == null || originalCreatedAt == null || absoluteExpiresAt == null
                || !absoluteExpiresAt.isAfter(originalCreatedAt)) { return false; }
        boolean empty = issuer == null && tenantId == null && subject == null;
        boolean complete = issuer != null && "https".equals(issuer.getScheme()) && issuer.getHost() != null
                && issuer.getUserInfo() == null && issuer.getQuery() == null && issuer.getFragment() == null
                && issuer.equals(issuer.normalize()) && tenantId != null && subject != null
                && !subject.isBlank() && subject.equals(subject.strip())
                && issuer.getPath() != null && issuer.getPath().endsWith("/tenants/" + tenantId);
        if (state == EdgeSessionBindingStateEnum.NEW) { return empty && closedAt == null; }
        if (state == EdgeSessionBindingStateEnum.ACTIVE) { return complete && closedAt == null; }
        return (empty || complete) && closedAt != null && !closedAt.isBefore(originalCreatedAt);
    }
}
