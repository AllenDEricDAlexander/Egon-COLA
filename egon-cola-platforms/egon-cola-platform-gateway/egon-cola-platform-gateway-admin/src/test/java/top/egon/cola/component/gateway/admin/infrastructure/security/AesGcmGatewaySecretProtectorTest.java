package top.egon.cola.component.gateway.admin.infrastructure.security;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.admin.application.credential.GatewaySecretProtector;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmGatewaySecretProtectorTest {

    @Test
    void encryptsWithAadAndDoesNotExposePlaintext() {
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) 7);
        AesGcmGatewaySecretProtector protector =
                new AesGcmGatewaySecretProtector(key, "v1");

        GatewaySecretProtector.ProtectedSecret encrypted =
                protector.protect("gateway-secret", "app:key");

        assertThat(encrypted.ciphertext())
                .doesNotContain("gateway-secret");
        assertThat(protector.unprotect(encrypted, "app:key"))
                .isEqualTo("gateway-secret");
        assertThatThrownBy(() -> protector.unprotect(
                encrypted,
                "other:key"
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
