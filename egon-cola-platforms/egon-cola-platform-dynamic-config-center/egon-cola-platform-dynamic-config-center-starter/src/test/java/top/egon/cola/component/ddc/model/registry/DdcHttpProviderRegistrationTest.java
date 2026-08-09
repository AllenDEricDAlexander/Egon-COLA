package top.egon.cola.component.ddc.model.registry;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.model.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.model.registry.DdcServiceKind;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcHttpProviderRegistrationTest {

    @Test
    void httpProviderKeyDefaultsOptionalIdentityAndRoundTrips() {
        DdcServiceKey serviceKey = new DdcServiceKey(
                        "pay-biz",
                        "orders-app",
                        "dev",
                        "default",
                        DdcServiceKind.HTTP_PROVIDER,
                "order-provider",
                null,
                null,
                "HTTP"
        );

        assertThat(serviceKey.group()).isEqualTo("default");
        assertThat(serviceKey.version()).isEqualTo("1.0.0");
        assertThat(serviceKey.protocol()).isEqualTo("http");
        assertThat(serviceKey.serviceKind().leaseRole())
                .isEqualTo(DdcLeaseRole.HTTP_PROVIDER);
        assertThat(DdcServiceKey.parse(serviceKey.canonicalValue()))
                .isEqualTo(serviceKey);
    }

    @Test
    void httpAndHttpsRequireMatchingSecureFlag() {
        DdcServiceRegistration http = registration("http", false);
        DdcServiceRegistration https = registration("https", true);

        assertThat(http.secure()).isFalse();
        assertThat(https.secure()).isTrue();
        assertThatThrownBy(() -> registration("http", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("secure");
        assertThatThrownBy(() -> registration("https", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("secure");
    }

    @Test
    void httpProviderRejectsNonHttpProtocol() {
        assertThatThrownBy(() -> registration("grpc", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("protocol");
    }

    private DdcServiceRegistration registration(
            String protocol,
            boolean secure) {
        return new DdcServiceRegistration(
                "http-provider-1",
                new DdcServiceKey(
                        "pay-biz",
                        "orders-app",
                        "dev",
                        "default",
                        DdcServiceKind.HTTP_PROVIDER,
                        "order-provider",
                        "default",
                        "1.0.0",
                        protocol
                ),
                "127.0.0.1",
                18081,
                secure,
                Map.of("gateway.zone", "cn-east"),
                30,
                10
        );
    }
}
