package top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class OAuthAuditBO {

    @Positive
    private Long id;
    @PositiveOrZero
    private long version;
    @NotBlank
    @Size(max = 64)
    private String eventId;
    @NotBlank
    @Size(max = 128)
    private String eventType;
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Size(max = 128)
    private String subjectRef;
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Size(max = 128)
    private String clientRef;
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Pattern(regexp = "[0-9a-f]{64}")
    @Size(max = 64)
    private String tokenRef;
    @Size(max = 2048)
    private String resourceRef;
    @NotBlank
    @Pattern(regexp = "SUCCESS|DENIED|FAILED|FAILURE")
    @Size(max = 16)
    private String result;
    @NotBlank
    @Size(max = 128)
    private String reasonCode;
    @Size(max = 128)
    private String traceId;
    @NotNull
    private Instant occurredAt;
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    @NotNull
    private JsonNode payload = JsonNodeFactory.instance.objectNode();
    @NotBlank
    @Pattern(regexp = "OAUTH|IDENTITY")
    @Size(max = 16)
    private String auditKind;
    @NotBlank
    @Size(max = 64)
    private String publicId;
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Size(max = 64)
    private String actorSub;
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Size(max = 64)
    private String targetSub;
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Size(max = 64)
    private String identityTenantId;
}
