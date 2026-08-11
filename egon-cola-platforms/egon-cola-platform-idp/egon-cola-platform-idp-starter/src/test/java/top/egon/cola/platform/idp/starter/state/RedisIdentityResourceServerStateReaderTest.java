package top.egon.cola.platform.idp.starter.state;

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
                {"resourceServerId":"resource-rbac3-prod",
                 "resourceUri":"https://api.example/prod/permission/rbac3",
                 "bizCode":"permission","appCode":"rbac3",
                 "environment":"prod","status":"ACTIVE","version":12}
                """);

        var state = reader.read("resource-rbac3-prod").orElseThrow();

        assertThat(state.resourceServerId())
                .isEqualTo("resource-rbac3-prod");
        assertThat(state.version()).isEqualTo(12L);
    }

    @Test
    void rejectsMalformedOrMismatchedResourceProjection() {
        assertThatThrownBy(() -> reader("not-json")
                .read("resource-rbac3-prod"))
                .isInstanceOf(RedisIdentityResourceServerStateReader
                        .StateUnavailableException.class);
        assertThatThrownBy(() -> reader("""
                {"resourceServerId":"resource-other",
                 "resourceUri":"https://api.example/other",
                 "bizCode":"permission","appCode":"other",
                 "environment":"prod","status":"ACTIVE","version":1}
                """).read("resource-rbac3-prod"))
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
