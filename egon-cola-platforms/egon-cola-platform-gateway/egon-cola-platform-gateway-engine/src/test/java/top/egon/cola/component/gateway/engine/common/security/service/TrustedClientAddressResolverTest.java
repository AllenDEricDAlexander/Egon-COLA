package top.egon.cola.component.gateway.engine.common.security.service;

import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrustedClientAddressResolverTest {

    @Test
    void onlyTrustedProxyCanSupplyForwardedAddress() {
        TrustedClientAddressResolver resolver =
                new TrustedClientAddressResolver(List.of(
                        "10.0.0.0/8",
                        "2001:db8::/32"
                ));
        Map<String, List<String>> headers = Map.of(
                "x-forwarded-for",
                List.of("203.0.113.9, 10.1.2.3")
        );

        assertEquals(
                "203.0.113.9",
                resolver.resolve(
                        new InetSocketAddress("10.2.3.4", 8080),
                        headers
                ).getHostAddress()
        );
        assertEquals(
                "192.0.2.10",
                resolver.resolve(
                        new InetSocketAddress("192.0.2.10", 8080),
                        headers
                ).getHostAddress()
        );
    }

    @Test
    void supportsStandardForwardedHeaderAndRejectsMalformedValue() {
        TrustedClientAddressResolver resolver =
                new TrustedClientAddressResolver(List.of("127.0.0.1/32"));

        assertEquals(
                "198.51.100.8",
                resolver.resolve(
                        new InetSocketAddress("127.0.0.1", 8080),
                        Map.of("forwarded", List.of(
                                "for=198.51.100.8;proto=https"
                        ))
                ).getHostAddress()
        );
        assertEquals(
                "127.0.0.1",
                resolver.resolve(
                        new InetSocketAddress("127.0.0.1", 8080),
                        Map.of("forwarded", List.of("for=unknown"))
                ).getHostAddress()
        );
    }
}
