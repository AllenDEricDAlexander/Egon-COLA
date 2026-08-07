package top.egon.cola.component.ddc.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.ddc.common.DdcErrorStatus;
import top.egon.cola.component.ddc.common.DdcException;
import top.egon.cola.component.ddc.config.DdcProperties;
import top.egon.cola.component.ddc.management.client.DdcClientTransportSecurity;
import top.egon.cola.component.ddc.management.client.DdcRestClientFactory;
import top.egon.cola.component.ddc.model.dto.DdcAckRequest;
import top.egon.cola.component.ddc.model.dto.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.dto.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.vo.DdcConfigValue;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;
import top.egon.cola.component.ddc.security.DdcCanonicalRequest;
import top.egon.cola.component.ddc.security.DdcRequestSigner;
import top.egon.cola.component.ddc.trace.DdcTraceSupport;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HttpDdcAdminClient implements DdcAdminClient {

    private final DdcProperties properties;

    private final RestClient restClient;

    private final ObjectMapper objectMapper;

    private final DdcRequestSigner signer = new DdcRequestSigner();

    public HttpDdcAdminClient(DdcProperties properties) {
        this(properties, restClientBuilder(properties));
    }

    private static RestClient.Builder restClientBuilder(
            DdcProperties properties) {
        DdcProperties.Admin admin = properties.getAdmin();
        String endpoint = admin.requireEndpoint();
        admin.validateCredentials();
        DdcProperties.Tls tls = admin.getTls();
        DdcClientTransportSecurity security =
                new DdcClientTransportSecurity(
                        tls.isEnabled(),
                        tls.isDevelopmentPlaintext(),
                        tls.getCertificateChainPath(),
                        tls.getPrivateKeyPath(),
                        tls.getTrustCertificateCollectionPath()
                );
        if (security.enabled()
                && !endpoint.startsWith("https://")) {
            throw new IllegalArgumentException(
                    "DDC mTLS endpoint must use HTTPS"
            );
        }
        if (!security.enabled()
                && !endpoint.startsWith("http://")) {
            throw new IllegalArgumentException(
                    "DDC HTTPS endpoint requires configured mTLS"
            );
        }
        return DdcRestClientFactory.create(
                admin.getConnectTimeout(),
                admin.getReadTimeout(),
                security
        );
    }

    HttpDdcAdminClient(DdcProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        DdcProperties.Admin admin = properties.getAdmin();
        String endpoint = admin.requireEndpoint();
        admin.validateCredentials();
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        this.restClient = restClientBuilder
                .baseUrl(endpoint)
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
        String path = "/api/v1/ddc/openapi/configs/pull";
        Map<String, List<String>> query = new LinkedHashMap<>();
        query.put("bizCode", List.of(properties.getBizCode()));
        query.put("appCode", List.of(properties.getAppCode()));
        query.put("env", List.of(properties.getEnv()));
        DdcCanonicalRequest canonicalRequest = canonicalRequest("GET", path, query, new byte[0]);
        ResultRecord<List<DdcConfigValue>> result = restClient.get()
                .uri(URI.create(path + "?" + canonicalRequest.canonicalQuery()))
                .headers(headers -> prepareHeaders(headers, canonicalRequest))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        List<DdcConfigValue> values = data(result, false);
        return values == null ? Collections.emptyList() : values;
    }

    @Override
    public void ack(DdcAckRequest request) {
        post(
                "/api/v1/ddc/openapi/publish/ack",
                request,
                new ParameterizedTypeReference<ResultRecord<Void>>() {
                },
                false
        );
    }

    private <T> T post(String path,
                       Object request,
                       ParameterizedTypeReference<ResultRecord<T>> responseType,
                       boolean required) {
        byte[] body = serialize(request);
        DdcCanonicalRequest canonicalRequest = canonicalRequest("POST", path, Map.of(), body);
        ResultRecord<T> result = restClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> prepareHeaders(headers, canonicalRequest))
                .body(body)
                .retrieve()
                .body(responseType);
        return data(result, required);
    }

    private <T> T data(ResultRecord<T> result, boolean required) {
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

    private byte[] serialize(Object request) {
        try {
            return objectMapper.writeValueAsBytes(request);
        } catch (JsonProcessingException exception) {
            throw new DdcException(DdcErrorStatus.INTERNAL_FAILURE, exception);
        }
    }

    private DdcCanonicalRequest canonicalRequest(String method,
                                                 String path,
                                                 Map<String, List<String>> query,
                                                 byte[] body) {
        return new DdcCanonicalRequest(
                method,
                path,
                query,
                System.currentTimeMillis(),
                UUID.randomUUID().toString(),
                body
        );
    }

    private void sign(HttpHeaders headers, DdcCanonicalRequest request) {
        if (!properties.getAdmin().isSignatureEnabled()) {
            return;
        }
        headers.set(DdcRequestSigner.ACCESS_KEY_HEADER, properties.getAdmin().getAccessKey());
        headers.set(DdcRequestSigner.TIMESTAMP_HEADER, Long.toString(request.timestamp()));
        headers.set(DdcRequestSigner.NONCE_HEADER, request.nonce());
        headers.set(DdcRequestSigner.CONTENT_SHA256_HEADER, request.contentSha256());
        headers.set(
                DdcRequestSigner.SIGNATURE_HEADER,
                signer.sign(request, properties.getAdmin().getSecretKey())
        );
    }

    private void prepareHeaders(HttpHeaders headers, DdcCanonicalRequest request) {
        DdcTraceSupport.inject(headers);
        sign(headers, request);
    }
}
