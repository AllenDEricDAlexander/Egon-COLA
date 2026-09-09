package top.egon.cola.platform.tianquan.shoubing.starter.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisIdentityResourceServerStateReaderTest {

    @Test
    void readsExactResourceProjection() {
        RedisIdentityResourceServerStateReader reader = reader("""
                {"resourceServerId":"resource-tianquan-jianshen-prod",
                 "resourceUri":"https://api.example/prod/permission/tianquan-jianshen",
                 "bizCode":"permission","appCode":"tianquan-jianshen",
                 "environment":"prod","status":"ACTIVE","version":12}
                """);

        var state = reader.read("resource-tianquan-jianshen-prod").orElseThrow();

        assertThat(state.resourceServerId())
                .isEqualTo("resource-tianquan-jianshen-prod");
        assertThat(state.version()).isEqualTo(12L);
    }

    @Test
    void rejectsMalformedOrMismatchedResourceProjection() {
        assertThatThrownBy(() -> reader("not-json")
                .read("resource-tianquan-jianshen-prod"))
                .isInstanceOf(RedisIdentityResourceServerStateReader
                        .StateUnavailableException.class);
        assertThatThrownBy(() -> reader("""
                {"resourceServerId":"resource-other",
                 "resourceUri":"https://api.example/other",
                 "bizCode":"permission","appCode":"other",
                 "environment":"prod","status":"ACTIVE","version":1}
                """).read("resource-tianquan-jianshen-prod"))
                .isInstanceOf(RedisIdentityResourceServerStateReader
                        .StateUnavailableException.class);
    }

    @SuppressWarnings("unchecked")
    private RedisIdentityResourceServerStateReader reader(String value) {
        RedissonClient redisson = mock(RedissonClient.class);
        RBucket<String> bucket = mock(RBucket.class);
        when(redisson.<String>getBucket(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any())).thenReturn(bucket);
        when(bucket.get()).thenReturn(value);
        return new RedisIdentityResourceServerStateReader(
                redisson,
                new ObjectMapper().findAndRegisterModules(),
                "identity:resource-server:"
        );
    }
}
