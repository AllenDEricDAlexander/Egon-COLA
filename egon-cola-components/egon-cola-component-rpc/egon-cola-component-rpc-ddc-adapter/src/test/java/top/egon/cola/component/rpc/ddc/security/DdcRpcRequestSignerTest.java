package top.egon.cola.component.rpc.ddc.security;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcScope;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.PullConfigRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.RegisterConfigClientRequest;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DdcRpcRequestSignerTest {

    private final DdcRpcRequestSigner signer = new DdcRpcRequestSigner();

    @Test
    void signsTheApprovedFiveLineCanonicalRequestWithoutTrailingLf() {
        PullConfigRequest request = PullConfigRequest.newBuilder()
                .setScope(DdcScope.newBuilder()
                        .setBizCode("retail")
                        .setEnv("prod")
                        .setAppCode("order"))
                .build();

        DdcRpcCanonicalRequest canonical = new DdcRpcCanonicalRequest(
                "egon.ddc.v1.DdcConfigRuntimeService/PullConfig",
                1700000000000L,
                "nonce-1",
                request
        );

        assertThat(canonical.contentSha256())
                .isEqualTo("a8eb4964d18ec3d881f6b27fa4b71c6f1d1283524fa44b449e830dd4b60973b0");
        assertThat(canonical.canonicalValue()).isEqualTo(String.join(
                "\n",
                "v1",
                "egon.ddc.v1.DdcConfigRuntimeService/PullConfig",
                "1700000000000",
                "nonce-1",
                "a8eb4964d18ec3d881f6b27fa4b71c6f1d1283524fa44b449e830dd4b60973b0"
        ));
        assertThat(canonical.canonicalValue()).doesNotEndWith("\n");
        assertThat(signer.sign(canonical, "secret"))
                .isEqualTo("09d08f69debf6faf553bf08f729e2af713b1aebe065ee0ef42ccd5681bea5571");
    }

    @Test
    void deterministicSerializationIgnoresMapInsertionOrder() {
        Map<String, String> forward = new LinkedHashMap<>();
        forward.put("zone", "east");
        forward.put("rack", "r1");
        Map<String, String> reverse = new LinkedHashMap<>();
        reverse.put("rack", "r1");
        reverse.put("zone", "east");

        RegisterConfigClientRequest first = request(forward);
        RegisterConfigClientRequest second = request(reverse);

        assertThat(DdcRpcCanonicalRequest.deterministicBytes(first))
                .containsExactly(DdcRpcCanonicalRequest.deterministicBytes(second));
        assertThat(new DdcRpcCanonicalRequest("service/Register", 1L, "n", first)
                .contentSha256())
                .isEqualTo(new DdcRpcCanonicalRequest("service/Register", 1L, "n", second)
                        .contentSha256());
    }

    private RegisterConfigClientRequest request(Map<String, String> metadata) {
        return RegisterConfigClientRequest.newBuilder()
                .setScope(DdcScope.newBuilder()
                        .setBizCode("retail")
                        .setEnv("prod")
                        .setAppCode("order"))
                .setInstanceId("instance-1")
                .setHost("127.0.0.1")
                .setPid("10")
                .setSdkVersion("1.0.0")
                .setLeaseSeconds(30)
                .setHeartbeatIntervalSeconds(10)
                .putAllMetadata(metadata)
                .build();
    }
}
