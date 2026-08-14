package top.egon.cola.platform.idp.admin.oauth.domain.vo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthUserTokenResultVOTest {

    @Test
    void exposesOnlyNonSecretUserTokenMetadata() throws Exception {
        var body = new ObjectMapper().readTree(
                new ObjectMapper().writeValueAsString(
                        new OAuthUserTokenResultVO("Bearer", 300L)));

        assertThat(body.get("token_type").asText()).isEqualTo("Bearer");
        assertThat(body.get("expires_in").asLong()).isEqualTo(300L);
        assertThat(body.has("access_token")).isFalse();
        assertThat(body.has("refresh_token")).isFalse();
    }
}
