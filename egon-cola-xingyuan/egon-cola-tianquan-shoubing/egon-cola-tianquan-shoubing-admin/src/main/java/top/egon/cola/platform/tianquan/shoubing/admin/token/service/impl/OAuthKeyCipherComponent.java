package top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;

@Component("oauthKeyCipherComponent")
@Slf4j
@RequiredArgsConstructor
@Validated
public class OAuthKeyCipherComponent {

    @Qualifier("oauthPayloadCipherComponent")
    private final OAuthPayloadCipherComponent payloadCipher;

    public String encryptPrivateKey(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,128}") String kid,
            @NotBlank @Size(max = 16_384) String privateKey) {
        Objects.requireNonNull(trustedRealm, "trustedRealm").validateTrustedPartition();
        String keyId = requiredKid(kid);
        if (privateKey == null || privateKey.isBlank() || privateKey.length() > 16_384) {
            throw new IllegalArgumentException("OAUTH_SIGNING_PRIVATE_KEY_INVALID");
        }
        return payloadCipher.encrypt(trustedRealm, "signing-key", keyId, privateKey);
    }

    public String decryptPrivateKey(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,128}") String kid,
            @NotBlank @Size(max = 32_768) String ciphertext) {
        Objects.requireNonNull(trustedRealm, "trustedRealm").validateTrustedPartition();
        String keyId = requiredKid(kid);
        if (ciphertext == null || ciphertext.isBlank() || ciphertext.length() > 32_768) {
            throw new IllegalArgumentException("OAUTH_SIGNING_PRIVATE_KEY_CIPHERTEXT_INVALID");
        }
        return payloadCipher.decrypt(trustedRealm, "signing-key", keyId, ciphertext);
    }

    private static String requiredKid(String kid) {
        if (kid == null || !kid.matches("[A-Za-z0-9_-]{1,128}")) {
            throw new IllegalArgumentException("OAUTH_SIGNING_KEY_KID_INVALID");
        }
        return kid;
    }
}
