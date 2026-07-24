package top.egon.cola.component.ddc.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;
import top.egon.cola.component.common.result.dto.ResultDto;
import top.egon.cola.component.common.crypto.hmac.Hmacs;
import top.egon.cola.component.ddc.common.DdcErrorStatus;
import top.egon.cola.component.ddc.common.DdcException;
import top.egon.cola.component.ddc.config.DdcProperties;
import top.egon.cola.component.ddc.model.dto.DdcAckRequest;
import top.egon.cola.component.ddc.model.dto.DdcDefaultReportRequest;
import top.egon.cola.component.ddc.model.dto.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.dto.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.vo.DdcConfigValue;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;

import java.util.Collections;
import java.util.List;

public class HttpDdcAdminClient implements DdcAdminClient {

    private final DdcProperties properties;

    private final RestClient restClient;

    public HttpDdcAdminClient(DdcProperties properties) {
        this(properties, RestClient.builder());
    }

    HttpDdcAdminClient(DdcProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        this.restClient = restClientBuilder
                .baseUrl(properties.getAdmin().getEndpoint())
                .messageConverters(converters -> {
                    converters.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
                    converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
                })
                .build();
    }

    @Override
    public DdcLeaseSession register(DdcInstanceRegisterRequest request) {
        return post(
                "/api/v1/ddc/openapi/instances/register",
                request,
                new ParameterizedTypeReference<>() {
                },
                true
        );
    }

    @Override
    public DdcLeaseOperationResult heartbeat(DdcHeartbeatRequest request) {
        return post(
                "/api/v1/ddc/openapi/instances/heartbeat",
                request,
                new ParameterizedTypeReference<>() {
                },
                true
        );
    }

    @Override
    public DdcLeaseOperationResult offline(DdcHeartbeatRequest request) {
        return post(
                "/api/v1/ddc/openapi/instances/offline",
                request,
                new ParameterizedTypeReference<>() {
                },
                true
        );
    }

    @Override
    public List<DdcConfigValue> pull() {
        ResultDto<List<DdcConfigValue>> result = restClient.get()
                .uri("/api/v1/ddc/openapi/configs/pull?appCode={appCode}&env={env}&namespace={namespace}",
                        properties.getAppCode(), properties.getEnv(), properties.getNamespace())
                .headers(headers -> sign(headers, "/api/v1/ddc/openapi/configs/pull"))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        List<DdcConfigValue> values = data(result, false);
        return values == null ? Collections.emptyList() : values;
    }

    @Override
    public void reportDefaults(DdcDefaultReportRequest request) {
        post(
                "/api/v1/ddc/openapi/defaults/report",
                request,
                new ParameterizedTypeReference<ResultDto<Void>>() {
                },
                false
        );
    }

    @Override
    public void ack(DdcAckRequest request) {
        post(
                "/api/v1/ddc/openapi/publish/ack",
                request,
                new ParameterizedTypeReference<ResultDto<Void>>() {
                },
                false
        );
    }

    String signature(String path, long timestamp) {
        String value = properties.getAdmin().getAccessKey() + "|" + timestamp + "|" + path;
        return Hmacs.sha256Hex(value, properties.getAdmin().getSecretKey());
    }

    private <T> T post(String path,
                       Object request,
                       ParameterizedTypeReference<ResultDto<T>> responseType,
                       boolean required) {
        ResultDto<T> result = restClient.post()
                .uri(path)
                .headers(headers -> sign(headers, path))
                .body(request)
                .retrieve()
                .body(responseType);
        return data(result, required);
    }

    private <T> T data(ResultDto<T> result, boolean required) {
        if (result == null) {
            throw new DdcException(DdcErrorStatus.INTERNAL_FAILURE);
        }
        if (!result.success()) {
            throw new DdcException(result.code(), result.status(), result.message());
        }
        if (required && result.data() == null) {
            throw new DdcException(DdcErrorStatus.INTERNAL_FAILURE);
        }
        return result.data();
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
