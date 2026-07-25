package top.egon.cola.component.gateway.admin.infrastructure.security;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayAdminHmacJwtDecoderTest {

    @Test
    void verifiesExplicitDevelopmentHmacTokens() throws Exception {
        byte[] secret =
                "gateway-live-jwt-secret-32-bytes!".getBytes(
                        StandardCharsets.UTF_8
                );
        String token = token(secret);
        var decoder = new GatewayAdminSecurityConfiguration()
                .gatewayAdminJwtDecoder(
                        "",
                        "",
                        Base64.getEncoder().encodeToString(secret)
                );

        assertThat(decoder.decode(token).getSubject())
                .isEqualTo("gateway-live-test");
    }

    private String token(byte[] secret) throws Exception {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString(
                "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(
                        StandardCharsets.UTF_8
                )
        );
        String payload = encoder.encodeToString(("""
                {"sub":"gateway-live-test","exp":%d,
                 "capabilities":["*"],"roles":["gateway-admin"]}
                """.formatted(
                Instant.parse("2099-01-01T00:00:00Z").getEpochSecond()
        )).getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return signingInput
                + "."
                + encoder.encodeToString(mac.doFinal(
                signingInput.getBytes(StandardCharsets.US_ASCII)
        ));
    }
}
