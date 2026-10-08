package top.egon.cola.platform.tianquan.shoubing.admin.token.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** Signing-key metadata view without private-key material. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SigningKeyVO {

    @NotBlank @Size(max = 128)
    private String kid;
    @NotBlank @Size(max = 16)
    private String algorithm;
    @NotBlank @Size(max = 32_768)
    private String publicJwk;
    @NotBlank @Size(max = 24)
    private String status;
    private boolean runtimeServing;
    private Instant activatedAt;
    private Instant retiredAt;
    @PositiveOrZero
    private long version;
    private Instant createdAt;
    private Instant updatedAt;
    @NotBlank @Size(max = 2048)
    private String issuer;
}
