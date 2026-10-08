package top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
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
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthKeyStatusEnum;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class OAuthSigningKeyBO {

    @Positive
    private Long id;
    @PositiveOrZero
    private long version;
    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9_-]{1,128}")
    @Size(max = 128)
    private String kid;
    @NotBlank
    @Pattern(regexp = "RS256")
    @Size(max = 16)
    private String algorithm;
    @NotNull
    private JsonNode publicJwk;
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @NotBlank
    @Pattern(regexp = "ENC:v1:[A-Za-z0-9][A-Za-z0-9._-]{0,63}:[A-Za-z0-9_-]{16}:[A-Za-z0-9_-]{22,}")
    @Size(max = 32_768)
    private String privateKeyCiphertext;
    @NotNull
    private OAuthKeyStatusEnum status;
    @NotNull
    private Instant publishedAt;
    private Instant activatedAt;
    private Instant lastIssuedAt;
    private Instant verifyUntil;
    private Instant retiredAt;
    private Instant createdAt;
    private Instant updatedAt;
}
