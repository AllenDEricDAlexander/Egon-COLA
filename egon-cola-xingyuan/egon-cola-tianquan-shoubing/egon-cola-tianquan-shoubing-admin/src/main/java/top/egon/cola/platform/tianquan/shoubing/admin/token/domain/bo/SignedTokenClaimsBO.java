package top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.util.Set;
import java.util.List;
import org.hibernate.validator.group.GroupSequenceProvider;
import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthTokenTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import top.egon.cola.platform.tianquan.shoubing.contract.OAuthSecurityVersionBO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@GroupSequenceProvider(SignedTokenClaimsBO.ClaimsGroupSequenceProvider.class)
public class SignedTokenClaimsBO {

    @NotNull
    @JsonProperty("iss")
    private URI issuer;

    @JsonProperty("aud")
    @NotEmpty
    @Size(max = 64)
    private Set<@NotNull URI> audience;

    @JsonProperty("sub")
    @NotBlank
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Size(max = 128)
    private String subject;

    @JsonProperty("scope")
    @NotNull(groups = AccessToken.class)
    @Size(max = 64)
    private Set<@NotBlank @Size(max = 128) String> scope;

    @JsonProperty("tenant_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @Positive
    private Long tenantId;

    @JsonProperty("principal_type")
    @NotBlank(groups = AccessToken.class)
    @Size(max = 16)
    private String principalType;

    @NotBlank
    @Pattern(regexp = "[0-9]{1,64}")
    @Size(max = 64)
    private String jti;

    @JsonProperty("iat")
    @NotNull
    private Instant issuedAt;

    @JsonProperty("nbf")
    @NotNull(groups = AccessToken.class)
    private Instant notBefore;

    @JsonProperty("exp")
    @NotNull
    private Instant expiresAt;

    @JsonProperty("client_id")
    @NotBlank(groups = AccessToken.class)
    @Size(max = 128)
    private String clientId;

    @JsonProperty("authorization_id")
    @NotBlank(groups = AccessToken.class)
    @Size(max = 32)
    private String authorizationId;

    @JsonProperty("client_registration_id")
    @NotBlank(groups = AccessToken.class)
    @Size(max = 128)
    private String clientRegistrationId;

    @JsonProperty("scope_context")
    @NotBlank(groups = AccessToken.class)
    @Size(max = 16)
    private String scopeContext;

    @JsonProperty("source_biz")
    @Size(max = 128)
    private String sourceBiz;

    @JsonProperty("source_app")
    @Size(max = 128)
    private String sourceApp;

    @JsonProperty("source_env")
    @Size(max = 128)
    private String sourceEnv;

    @JsonProperty("app_id")
    @Size(max = 128)
    private String appId;

    @JsonProperty("credential_id")
    @Size(max = 128)
    private String credentialId;

    @JsonProperty("resource_version")
    @PositiveOrZero
    private Long resourceVersion;

    @JsonProperty("acr")
    @Size(max = 64)
    private String acr;

    @JsonProperty("auth_time")
    private Instant authTime;

    @JsonProperty("security_version")
    @NotNull(groups = AccessToken.class)
    @Valid
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private OAuthSecurityVersionBO securityVersion;
    @JsonProperty("_token_use")
    @NotNull
    @Builder.Default
    private OAuthTokenTypeEnum tokenUse = OAuthTokenTypeEnum.ACCESS;

    @JsonProperty("nonce")
    @Size(max = 512)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String nonce;

    @JsonProperty("azp")
    @NotBlank(groups = IdToken.class)
    @Size(max = 128)
    private String authorizedParty;

    @JsonProperty("sid")
    @Size(max = 256)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String sessionId;

    @JsonIgnore
    @AssertTrue
    public boolean isSignedTokenUse() {
        return tokenUse == OAuthTokenTypeEnum.ACCESS || tokenUse == OAuthTokenTypeEnum.ID;
    }

    public interface AccessToken { }
    public interface IdToken { }

    public static final class ClaimsGroupSequenceProvider
            implements DefaultGroupSequenceProvider<SignedTokenClaimsBO> {
        @Override
        public List<Class<?>> getValidationGroups(SignedTokenClaimsBO claims) {
            return List.of(SignedTokenClaimsBO.class,
                    claims != null && claims.getTokenUse() == OAuthTokenTypeEnum.ID
                            ? IdToken.class : AccessToken.class);
        }
    }

}
