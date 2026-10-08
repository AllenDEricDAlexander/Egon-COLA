package top.egon.cola.platform.tianquan.shoubing.starter.security.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.security.oauth2.jwt.Jwt;
import top.egon.cola.platform.tianquan.shoubing.contract.OAuthSecurityVersionBO;
import top.egon.cola.platform.tianquan.shoubing.contract.PrincipalType;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry;

/** Holds claims only after a configured issuer's local decoder has verified the JWT. */
@Data
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class VerifiedJwtBO {

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Jwt jwt;

    @NotNull
    @Valid
    private TrustedIssuerRegistry.TrustedIssuerVO trustedIssuer;

    @NotBlank
    private String subject;

    private String tenantId;

    @NotBlank
    private String tokenId;

    @NotBlank
    private String keyId;

    @NotBlank
    private String authorizationId;

    @NotBlank
    private String clientRegistrationId;

    @NotNull
    private PrincipalType principalType;

    @NotNull
    private Instant expiresAt;

    @NotNull
    @Valid
    private OAuthSecurityVersionBO securityVersion;

    public void validate() {
        Objects.requireNonNull(jwt, "jwt");
        Objects.requireNonNull(trustedIssuer, "trustedIssuer");
        required(subject, "subject");
        if (!required(tokenId, "tokenId").matches("[1-9][0-9]{0,18}")) {
            throw new IllegalArgumentException("tokenId must be a positive decimal id");
        }
        if (!required(keyId, "keyId").matches("[A-Za-z0-9._-]{1,128}")) {
            throw new IllegalArgumentException("keyId is invalid");
        }
        positiveDecimal(authorizationId, "authorizationId");
        positiveDecimal(clientRegistrationId, "clientRegistrationId");
        Objects.requireNonNull(principalType, "principalType");
        Objects.requireNonNull(expiresAt, "expiresAt");
        Objects.requireNonNull(securityVersion, "securityVersion");
        if (principalType == PrincipalType.USER && (tenantId == null || tenantId.isBlank())) {
            throw new IllegalArgumentException("USER tenantId is required");
        }
        if (principalType == PrincipalType.USER && securityVersion.getSubject() == null) {
            throw new IllegalArgumentException("USER subject security version is required");
        }
        if (principalType == PrincipalType.SERVICE && securityVersion.getSubject() != null) {
            throw new IllegalArgumentException("SERVICE must not have a subject security version");
        }
        if (principalType == PrincipalType.SERVICE
                && trustedIssuer.tenantId() == null && tenantId != null) {
            throw new IllegalArgumentException("PLATFORM SERVICE tenantId must be absent");
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static void positiveDecimal(String value, String field) {
        String normalized = required(value, field);
        if (!normalized.matches("[1-9][0-9]{0,18}")) {
            throw new IllegalArgumentException(field + " must be a positive decimal id");
        }
    }
}
