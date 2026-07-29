package top.egon.cola.component.accessguard.key;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.core.GuardEntryType;
import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.core.GuardInvocationKind;
import top.egon.cola.component.accessguard.core.plan.KeyConfig;
import top.egon.cola.component.accessguard.key.contributor.ClientIpKeyContributor;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpKeyContributorTest {

    @Test
    void ignoresForwardedForFromUntrustedRemoteAddress() throws Exception {
        FakeRequest request = new FakeRequest();
        request.setRemoteAddr("203.0.113.7");
        request.addHeader("X-Forwarded-For", "10.0.0.8");
        ClientIpKeyContributor contributor = new ClientIpKeyContributor(new TrustedProxyMatcher(List.of("10.0.0.0/8")));

        assertThat(contributor.contribute(invocation(request), config()))
                .containsExactly(new GuardKeyPart("ip", "203.0.113.7", 0));
    }

    @Test
    void acceptsForwardedForOnlyFromTrustedRemoteAddress() throws Exception {
        FakeRequest request = new FakeRequest();
        request.setRemoteAddr("10.1.2.3");
        request.addHeader("X-Forwarded-For", "198.51.100.9, 10.1.2.2");
        ClientIpKeyContributor contributor = new ClientIpKeyContributor(new TrustedProxyMatcher(List.of("10.0.0.0/8")));

        assertThat(contributor.contribute(invocation(request), config()))
                .containsExactly(new GuardKeyPart("ip", "198.51.100.9", 0));
    }

    private static KeyConfig config() {
        return new KeyConfig(List.of("CLIENT_IP"), List.of("10.0.0.0/8"), "test-secret", List.of(), 128);
    }

    private static GuardInvocation invocation(FakeRequest request) throws Exception {
        Method method = Sample.class.getDeclaredMethod("draw");
        return new GuardInvocation(
                "draw",
                new Sample(),
                Sample.class,
                method,
                new Object[0],
                Map.of(ClientIpKeyContributor.HTTP_REQUEST_ATTRIBUTE, request),
                GuardEntryType.AOP,
                GuardInvocationKind.METHOD,
                () -> "ok");
    }

    static class Sample {

        String draw() {
            return "ok";
        }
    }

    public static final class FakeRequest {

        private final Map<String, String> headers = new ConcurrentHashMap<>();
        private String remoteAddr;

        public String getRemoteAddr() {
            return remoteAddr;
        }

        public void setRemoteAddr(String remoteAddr) {
            this.remoteAddr = remoteAddr;
        }

        public String getHeader(String name) {
            return headers.get(name);
        }

        public void addHeader(String name, String value) {
            headers.put(name, value);
        }
    }
}
