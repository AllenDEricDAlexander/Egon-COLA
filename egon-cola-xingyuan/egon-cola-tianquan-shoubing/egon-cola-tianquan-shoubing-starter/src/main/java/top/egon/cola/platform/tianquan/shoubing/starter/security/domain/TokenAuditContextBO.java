package top.egon.cola.platform.tianquan.shoubing.starter.security.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

/** Server-owned context for the existing eleven-field usage Event. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class TokenAuditContextBO {
    @Valid private TokenAuditProofBO proof;
    @JsonIgnore @ToString.Exclude private VerifiedJwtBO verifiedJwt;
    @NotBlank @Pattern(regexp = "[A-Za-z0-9._~-]{1,64}") private String eventId;
    @NotNull private URI fallbackIssuer;
    @NotNull private URI resource;
    @NotBlank @Pattern(regexp = "SUCCESS|DENIED|FAILED") private String result;
    @NotBlank @Pattern(regexp = "[A-Z0-9_]{1,128}") private String reasonCode;
    @NotNull private Instant occurredAt;
    @Pattern(regexp = "[A-Za-z0-9._~-]{1,128}") private String traceId;
}
