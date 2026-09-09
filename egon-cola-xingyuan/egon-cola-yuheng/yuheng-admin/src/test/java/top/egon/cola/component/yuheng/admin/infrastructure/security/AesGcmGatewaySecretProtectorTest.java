package top.egon.cola.component.yuheng.admin.credential.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.yuheng.admin.credential.service.GatewaySecretProtector;

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

        top.egon.cola.component.yuheng.admin.credential.domain.vo.GatewayProtectedSecretVO encrypted =
                protector.protect("yuheng-secret", "app:key");

        assertThat(encrypted.ciphertext())
                .doesNotContain("yuheng-secret");
        assertThat(protector.unprotect(encrypted, "app:key"))
                .isEqualTo("yuheng-secret");
        assertThatThrownBy(() -> protector.unprotect(
                encrypted,
                "other:key"
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
