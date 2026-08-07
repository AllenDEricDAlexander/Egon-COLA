package top.egon.cola.component.ddc.security;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DdcRequestSignerTest {

    private final DdcRequestSigner signer = new DdcRequestSigner();

    @Test
    void canonicalizesSortedAndPercentEncodedGetQueryWithEmptyBody() {
        DdcCanonicalRequest request = new DdcCanonicalRequest(
                "get",
                "/api/v1/ddc/openapi/configs/pull",
                Map.of(
                        "z", List.of("last"),
                        "space", List.of("a b"),
                        "symbol", List.of("/?"),
                        "a", List.of("2", "1")
                ),
                1700000000000L,
                "nonce-1",
                new byte[0]
        );

        assertThat(request.canonicalQuery())
                .isEqualTo("a=1&a=2&space=a%20b&symbol=%2F%3F&z=last");
        assertThat(request.canonicalValue()).isEqualTo("""
                GET
                /api/v1/ddc/openapi/configs/pull
                a=1&a=2&space=a%20b&symbol=%2F%3F&z=last
                1700000000000
                nonce-1
                e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855""");
        assertThat(signer.sign(request, "secret"))
                .isEqualTo("f21c7e50c73a0f6c04f2bde8bc6232249029f75c970a76a9b8e4697121a64d7c");
    }

    @Test
    void signsTheExactJsonBytesAndDetectsOneByteTampering() {
        byte[] body = "{\"value\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
        DdcCanonicalRequest request = request(body);
        String signature = signer.sign(request, "secret");

        assertThat(request.contentSha256())
                .isEqualTo("16cfa6ba3d308e6c52a96d7d50018be09d175a518b74d5bcc6e39281ef75fa9b");
        assertThat(signer.matches(signature, signer.sign(request, "secret"))).isTrue();

        byte[] tamperedBody = body.clone();
        tamperedBody[tamperedBody.length - 2] = 'n';
        assertThat(signer.matches(signature, signer.sign(request(tamperedBody), "secret"))).isFalse();
    }

    private DdcCanonicalRequest request(byte[] body) {
        return new DdcCanonicalRequest(
                "POST",
                "/api/v1/ddc/openapi/publish/ack",
                Map.of(),
                1700000000000L,
                "nonce-1",
                body
        );
    }
}
