package top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class OAuthTokenMetadataBO {

    @Min(1)
    @Max(1)
    @Builder.Default
    private int schemaVersion = 1;

    @Valid
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SignedTokenClaimsBO claims;

    private boolean invalidated;
    private boolean codeConsumed;
}
