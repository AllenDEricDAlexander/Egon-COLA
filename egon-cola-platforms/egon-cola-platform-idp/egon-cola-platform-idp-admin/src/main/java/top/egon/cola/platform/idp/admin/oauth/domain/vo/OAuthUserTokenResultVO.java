package top.egon.cola.platform.idp.admin.oauth.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Non-secret response metadata for a browser USER token operation.
 *
 * <p>The USER access and refresh tokens are written only to HttpOnly cookies. This response
 * deliberately contains metadata and never contains either raw token.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OAuthUserTokenResultVO(
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn
) {

    public OAuthUserTokenResultVO {
        tokenType = Objects.requireNonNull(tokenType, "tokenType");
        if (tokenType.isBlank() || !tokenType.equals(tokenType.trim())) {
            throw new IllegalArgumentException("tokenType is invalid");
        }
        if (expiresIn < 0L) {
            throw new IllegalArgumentException("expiresIn must not be negative");
        }
    }
}
