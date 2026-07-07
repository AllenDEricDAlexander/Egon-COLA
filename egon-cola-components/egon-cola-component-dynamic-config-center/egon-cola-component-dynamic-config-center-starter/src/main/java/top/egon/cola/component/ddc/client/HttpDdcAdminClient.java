package top.egon.cola.component.ddc.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import top.egon.cola.component.common.result.Result;
import top.egon.cola.component.common.util.CryptoUtils;
import top.egon.cola.component.ddc.config.DdcProperties;
import top.egon.cola.component.ddc.model.dto.DdcAckRequest;
import top.egon.cola.component.ddc.model.dto.DdcDefaultReportRequest;
import top.egon.cola.component.ddc.model.dto.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.dto.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.vo.DdcConfigValue;

import java.util.Collections;
import java.util.List;

public class HttpDdcAdminClient implements DdcAdminClient {

    private final DdcProperties properties;

    private final RestClient restClient;

    public HttpDdcAdminClient(DdcProperties properties) {
        this(properties, RestClient.builder().baseUrl(properties.getAdmin().getEndpoint()).build());
    }

    HttpDdcAdminClient(DdcProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public void register(DdcInstanceRegisterRequest request) {
        post("/api/v1/ddc/openapi/instances/register", request);
    }

    @Override
    public void heartbeat(DdcHeartbeatRequest request) {
        post("/api/v1/ddc/openapi/instances/heartbeat", request);
    }

    @Override
    public void offline(DdcHeartbeatRequest request) {
        post("/api/v1/ddc/openapi/instances/offline", request);
    }

    @Override
    public List<DdcConfigValue> pull() {
        Result<List<DdcConfigValue>> result = restClient.get()
                .uri("/api/v1/ddc/openapi/configs/pull?appCode={appCode}&env={env}&namespace={namespace}",
                        properties.getAppCode(), properties.getEnv(), properties.getNamespace())
                .headers(headers -> sign(headers, "/api/v1/ddc/openapi/configs/pull"))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        return result == null || result.getData() == null ? Collections.emptyList() : result.getData();
    }

    @Override
    public void reportDefaults(DdcDefaultReportRequest request) {
        post("/api/v1/ddc/openapi/defaults/report", request);
    }

    @Override
    public void ack(DdcAckRequest request) {
        post("/api/v1/ddc/openapi/publish/ack", request);
    }

    String signature(String path, long timestamp) {
        String value = properties.getAdmin().getAccessKey() + "|" + timestamp + "|" + path;
        return CryptoUtils.hmacSha256Hex(value, properties.getAdmin().getSecretKey());
    }

    private void post(String path, Object request) {
        restClient.post()
                .uri(path)
                .headers(headers -> sign(headers, path))
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    private void sign(HttpHeaders headers, String path) {
        if (!properties.getAdmin().isSignatureEnabled()) {
            return;
        }
        long timestamp = System.currentTimeMillis();
        headers.add("X-DDC-Access-Key", properties.getAdmin().getAccessKey());
        headers.add("X-DDC-Timestamp", String.valueOf(timestamp));
        headers.add("X-DDC-Signature", signature(path, timestamp));
    }
}
