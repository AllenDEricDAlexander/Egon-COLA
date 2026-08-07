package top.egon.cola.component.ddc.admin.security.management;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import top.egon.cola.component.ddc.admin.config.DdcAdminProperties;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcAdminJwtDecoderTest {

    private static final byte[] SECRET =
            "ddc-admin-jwt-secret-at-least-32-bytes"
                    .getBytes(StandardCharsets.UTF_8);

    @Test
    void verifiesIssuerAndAudienceForExplicitHmacTokens()
            throws Exception {
        var decoder = decoder();

        assertThat(decoder.decode(token("ddc-admin")).getSubject())
                .isEqualTo("admin-42");
        assertThatThrownBy(() -> decoder.decode(token("other-service")))
                .isInstanceOf(JwtValidationException.class);
    }

    private org.springframework.security.oauth2.jwt.JwtDecoder decoder() {
        DdcAdminProperties properties = new DdcAdminProperties();
        properties.getSecurity().setLocalDev(true);
        properties.getSecurity().getJwt().setIssuer("test-issuer");
        properties.getSecurity().getJwt().setAudience("ddc-admin");
        properties.getSecurity().getJwt().setHmacSecretBase64(
                Base64.getEncoder().encodeToString(SECRET)
        );
        return new DdcAdminSecurityConfiguration()
                .ddcAdminJwtDecoder(properties);
    }

    private String token(String audience) throws Exception {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString(
                "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(
                        StandardCharsets.UTF_8
                )
        );
        String payload = encoder.encodeToString(("""
                {"sub":"admin-42","iss":"test-issuer","aud":"%s",
                 "exp":%d,"capabilities":["DDC_READ"]}
                """.formatted(
                audience,
                Instant.parse("2099-01-01T00:00:00Z").getEpochSecond()
        )).getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET, "HmacSHA256"));
        return signingInput
                + "."
                + encoder.encodeToString(mac.doFinal(
                signingInput.getBytes(StandardCharsets.US_ASCII)
        ));
    }
}
