package top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
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

/** One-time client creation result. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreatedOAuthClientVO {

    @NotBlank
    @Size(max = 128)
    private String clientId;
    @Size(max = 128)
    private String appId;
    @NotBlank
    @Size(max = 200)
    private String clientName;
    @NotBlank
    @Size(max = 16)
    private String clientType;
    @NotBlank
    @Size(max = 16)
    private String purpose;
    @NotBlank
    @Size(max = 16)
    private String status;
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Size(max = 128)
    private String clientSecret;
    @Size(max = 8)
    private String secretHint;
    @PositiveOrZero
    private long version;
    private Instant createdAt;
    @NotBlank
    @Size(max = 2048)
    private String issuer;
}
