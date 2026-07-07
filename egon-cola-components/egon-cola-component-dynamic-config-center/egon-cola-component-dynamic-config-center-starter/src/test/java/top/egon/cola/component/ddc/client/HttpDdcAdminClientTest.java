package top.egon.cola.component.ddc.client;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.util.CryptoUtils;
import top.egon.cola.component.ddc.config.DdcProperties;

import static org.assertj.core.api.Assertions.assertThat;

class HttpDdcAdminClientTest {

    @Test
    void signatureUsesAccessKeyTimestampPathAndSecret() {
        DdcProperties properties = new DdcProperties();
        properties.getAdmin().setAccessKey("ak");
        properties.getAdmin().setSecretKey("sk");
        HttpDdcAdminClient client = new HttpDdcAdminClient(properties);

        String signature = client.signature("/api/v1/ddc/openapi/publish/ack", 100L);

        assertThat(signature).isEqualTo(CryptoUtils.hmacSha256Hex("ak|100|/api/v1/ddc/openapi/publish/ack", "sk"));
    }
}
