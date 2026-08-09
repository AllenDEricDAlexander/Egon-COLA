package top.egon.cola.component.gateway.contract.reporting;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayRequestSignerTest {

    private final GatewayRequestSigner signer = new GatewayRequestSigner();

    @Test
    void canonicalizesAndSignsGatewayReportRequests() {
        GatewayCanonicalRequest request = new GatewayCanonicalRequest(
                "post",
                "/api/v1/gateway/openapi/interface-definitions/reports",
                Map.of(
                        "z", List.of("last"),
                        "space", List.of("a b"),
                        "a", List.of("2", "1")
                ),
                1700000000000L,
                "nonce-1",
                "{}".getBytes(StandardCharsets.UTF_8)
        );

        assertEquals(
                "a=1&a=2&space=a%20b&z=last",
                request.canonicalQuery()
        );
        String signature = signer.sign(request, "secret");
        assertTrue(signer.matches(
                signature,
                signer.sign(request, "secret")
        ));
        assertFalse(signer.matches(signature, signer.sign(
                new GatewayCanonicalRequest(
                        "POST",
                        "/api/v1/gateway/openapi/interface-definitions/reports",
                        Map.of(
                                "z", List.of("last"),
                                "space", List.of("a b"),
                                "a", List.of("2", "1")
                        ),
                        1700000000000L,
                        "nonce-1",
                        "{\"changed\":true}"
                                .getBytes(StandardCharsets.UTF_8)
                ),
                "secret"
        )));
    }
}
