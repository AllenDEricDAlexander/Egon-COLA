package top.egon.cola.platform.tianquan.shoubing.starter.audit.domain.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.Accessors;

/** Signed confirmation of the consumer's committed fact, never an authorization credential. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder @Accessors(chain = true)
@JsonPropertyOrder({"schemaVersion", "eventId", "eventFingerprint", "outcome", "confirmedAt", "signature"})
public class TokenUsageAuditReceiptDTO {
    @Min(1) @Max(1) private int schemaVersion;
    @NotBlank @Pattern(regexp="[A-Za-z0-9._~-]{1,64}") private String eventId;
    @NotBlank @Pattern(regexp="[0-9a-f]{64}") private String eventFingerprint;
    @NotBlank @Pattern(regexp="STORED|EXPIRED|DEAD") private String outcome;
    @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING) private Instant confirmedAt;
    @NotBlank @Pattern(regexp="[0-9a-f]{64}") private String signature;
}
