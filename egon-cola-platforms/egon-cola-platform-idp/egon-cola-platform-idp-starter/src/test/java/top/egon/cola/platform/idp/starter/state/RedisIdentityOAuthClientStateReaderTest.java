package top.egon.cola.platform.idp.starter.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import top.egon.cola.platform.idp.core.oauth.OAuthClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisIdentityOAuthClientStateReaderTest {

    @Test
    void readsExactConfidentialClientProjection() {
        RedisIdentityOAuthClientStateReader reader = reader("""
                {"clientId":"rbac3-service","clientType":"CONFIDENTIAL",
                 "status":"ACTIVE",
                 "boundSourceResourceServerId":"resource-idp-prod",
                 "version":3}
                """);

        var state = reader.read("rbac3-service").orElseThrow();

        assertThat(state.clientType())
                .isEqualTo(OAuthClient.ClientType.CONFIDENTIAL);
        assertThat(state.boundSourceResourceServerId())
                .isEqualTo("resource-idp-prod");
    }

    @Test
    void rejectsMalformedOrMismatchedClientProjection() {
        assertThatThrownBy(() -> reader("not-json")
                .read("rbac3-service"))
                .isInstanceOf(RedisIdentityOAuthClientStateReader
                        .StateUnavailableException.class);
        assertThatThrownBy(() -> reader("""
                {"clientId":"other-service","clientType":"CONFIDENTIAL",
                 "status":"ACTIVE",
                 "boundSourceResourceServerId":"resource-other-prod",
                 "version":1}
                """).read("rbac3-service"))
                .isInstanceOf(RedisIdentityOAuthClientStateReader
                        .StateUnavailableException.class);
    }

    @SuppressWarnings("unchecked")
    private RedisIdentityOAuthClientStateReader reader(String value) {
        RedissonClient redisson = mock(RedissonClient.class);
        RBucket<String> bucket = mock(RBucket.class);
        when(redisson.<String>getBucket(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any())).thenReturn(bucket);
        when(bucket.get()).thenReturn(value);
        return new RedisIdentityOAuthClientStateReader(
                redisson,
                new ObjectMapper().findAndRegisterModules(),
                "identity:oauth-client:"
        );
    }
}
