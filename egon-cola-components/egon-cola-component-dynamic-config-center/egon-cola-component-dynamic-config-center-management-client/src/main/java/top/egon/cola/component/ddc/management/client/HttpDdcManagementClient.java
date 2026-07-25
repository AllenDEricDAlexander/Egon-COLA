package top.egon.cola.component.ddc.management.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriUtils;
import top.egon.cola.component.common.result.dto.ResultDto;
import top.egon.cola.component.ddc.management.DdcManagementClient;
import top.egon.cola.component.ddc.management.model.DdcManagementConfig;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigClientInstance;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigDeleteRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigUpsertRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementInstanceQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishResult;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishTask;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceCatalog;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceSnapshot;
import top.egon.cola.component.ddc.security.DdcCanonicalRequest;
import top.egon.cola.component.ddc.security.DdcRequestSigner;

import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class HttpDdcManagementClient implements DdcManagementClient {

    private static final String MANAGEMENT_PATH = "/api/v1/ddc/openapi/management";

    private final DdcManagementClientProperties properties;

    private final RestClient restClient;

    private final ObjectMapper objectMapper;

    private final Clock clock;

    private final Supplier<String> nonceSupplier;

    private final DdcRequestSigner signer = new DdcRequestSigner();

    public HttpDdcManagementClient(DdcManagementClientProperties properties) {
        this(
                properties,
                defaultBuilder(properties),
                Clock.systemUTC(),
                () -> UUID.randomUUID().toString()
        );
    }

    HttpDdcManagementClient(
            DdcManagementClientProperties properties,
            RestClient.Builder restClientBuilder,
            Clock clock,
            Supplier<String> nonceSupplier
    ) {
        this.properties = require(properties, "properties");
        this.clock = require(clock, "clock");
        this.nonceSupplier = require(nonceSupplier, "nonceSupplier");
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.restClient = require(restClientBuilder, "restClientBuilder")
                .baseUrl(properties.endpoint())
                .messageConverters(converters -> {
                    converters.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
                    converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
                })
                .build();
    }

    @Override
    public DdcManagementConfig upsert(DdcManagementConfigUpsertRequest request) {
        require(request, "request");
        return exchange(
                HttpMethod.PUT,
                configPath(
                        request.appCode(),
                        request.env(),
                        request.namespace(),
                        request.configKey()
                ),
                Map.of(),
                request,
                new ParameterizedTypeReference<>() {
                },
                true
        );
    }

    @Override
    public void delete(DdcManagementConfigDeleteRequest request) {
        require(request, "request");
        exchange(
                HttpMethod.DELETE,
                configPath(
                        request.appCode(),
                        request.env(),
                        request.namespace(),
                        request.configKey()
                ),
                Map.of(),
                request,
                new ParameterizedTypeReference<ResultDto<Void>>() {
                },
                false
        );
    }

    @Override
    public DdcManagementPublishResult publish(DdcManagementPublishRequest request) {
        require(request, "request");
        return exchange(
                HttpMethod.POST,
                configPath(
                        request.appCode(),
                        request.env(),
                        request.namespace(),
                        request.configKey()
                ) + "/publish",
                Map.of(),
                request,
                new ParameterizedTypeReference<>() {
                },
                true
        );
    }

    @Override
    public DdcManagementPublishTask getPublishTask(String changeId) {
        return exchange(
                HttpMethod.GET,
                MANAGEMENT_PATH + "/publish-tasks/" + segment(changeId, "changeId"),
                Map.of(),
                null,
                new ParameterizedTypeReference<>() {
                },
                true
        );
    }

    @Override
    public DdcManagementPublishResult retry(String changeId) {
        return exchange(
                HttpMethod.POST,
                MANAGEMENT_PATH + "/publish-tasks/" + segment(changeId, "changeId") + "/retry",
                Map.of(),
                null,
                new ParameterizedTypeReference<>() {
                },
                true
        );
    }

    @Override
    public List<DdcManagementConfigClientInstance> getConfigClients(
            DdcManagementInstanceQuery query
    ) {
        require(query, "query");
        return exchange(
                HttpMethod.GET,
                MANAGEMENT_PATH + "/instances",
                configClientQuery(query),
                null,
                new ParameterizedTypeReference<>() {
                },
                true
        );
    }

    @Override
    public DdcManagementServiceCatalog getServiceKeys(DdcManagementServiceQuery query) {
        require(query, "query");
        return exchange(
                HttpMethod.GET,
                MANAGEMENT_PATH + "/registry/services",
                serviceQuery(query),
                null,
                new ParameterizedTypeReference<>() {
                },
                true
        );
    }

    @Override
    public DdcManagementServiceSnapshot getInstances(DdcManagementServiceQuery query) {
        require(query, "query");
        return exchange(
                HttpMethod.GET,
                MANAGEMENT_PATH + "/registry/instances",
                serviceQuery(query),
                null,
                new ParameterizedTypeReference<>() {
                },
                true
        );
    }

    private <T> T exchange(
            HttpMethod method,
            String path,
            Map<String, List<String>> query,
            Object request,
            ParameterizedTypeReference<ResultDto<T>> responseType,
            boolean required
    ) {
        byte[] body = request == null ? new byte[0] : serialize(request);
        DdcCanonicalRequest canonicalRequest = new DdcCanonicalRequest(
                method.name(),
                path,
                query,
                clock.millis(),
                requireText(nonceSupplier.get(), "nonce"),
                body
        );
        String requestTarget = canonicalRequest.canonicalQuery().isEmpty()
                ? path
                : path + "?" + canonicalRequest.canonicalQuery();
        try {
            RestClient.RequestBodySpec spec = restClient.method(method)
                    .uri(URI.create(requestTarget))
                    .headers(headers -> sign(headers, canonicalRequest));
            if (request != null) {
                spec.contentType(MediaType.APPLICATION_JSON).body(body);
            }
            ResultDto<T> result = spec.retrieve().body(responseType);
            return data(result, required);
        } catch (DdcManagementClientException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new DdcManagementClientException(
                    "DDC_MANAGEMENT_IO_ERROR",
                    "DDC management request failed",
                    exception
            );
        }
    }

    private <T> T data(ResultDto<T> result, boolean required) {
        if (result == null) {
            throw new DdcManagementClientException(
                    "DDC_MANAGEMENT_EMPTY_RESPONSE",
                    "DDC management returned an empty response",
                    null
            );
        }
        if (!result.success()) {
            throw new DdcManagementClientException(
                    result.code(),
                    result.status(),
                    result.message()
            );
        }
        if (required && result.data() == null) {
            throw new DdcManagementClientException(
                    "DDC_MANAGEMENT_EMPTY_DATA",
                    "DDC management returned empty data",
                    null
            );
        }
        return result.data();
    }

    private byte[] serialize(Object request) {
        try {
            return objectMapper.writeValueAsBytes(request);
        } catch (JsonProcessingException exception) {
            throw new DdcManagementClientException(
                    "DDC_MANAGEMENT_SERIALIZATION_ERROR",
                    "DDC management request serialization failed",
                    exception
            );
        }
    }

    private void sign(HttpHeaders headers, DdcCanonicalRequest request) {
        headers.set(DdcRequestSigner.ACCESS_KEY_HEADER, properties.accessKey());
        headers.set(DdcRequestSigner.TIMESTAMP_HEADER, Long.toString(request.timestamp()));
        headers.set(DdcRequestSigner.NONCE_HEADER, request.nonce());
        headers.set(DdcRequestSigner.CONTENT_SHA256_HEADER, request.contentSha256());
        headers.set(
                DdcRequestSigner.SIGNATURE_HEADER,
                signer.sign(request, properties.secretKey())
        );
    }

    private String configPath(
            String appCode,
            String env,
            String namespace,
            String configKey
    ) {
        return MANAGEMENT_PATH
                + "/configs/"
                + segment(appCode, "appCode") + "/"
                + segment(env, "env") + "/"
                + segment(namespace, "namespace") + "/"
                + segment(configKey, "configKey");
    }

    private Map<String, List<String>> configClientQuery(DdcManagementInstanceQuery query) {
        Map<String, List<String>> values = new LinkedHashMap<>();
        putQuery(values, "appCode", query.appCode());
        putQuery(values, "env", query.env());
        putQuery(values, "namespace", query.namespace());
        return values;
    }

    private Map<String, List<String>> serviceQuery(DdcManagementServiceQuery query) {
        Map<String, List<String>> values = new LinkedHashMap<>();
        putQuery(values, "env", query.env());
        putQuery(values, "namespace", query.namespace());
        putQuery(values, "serviceKind", query.serviceKind());
        putQuery(values, "protocol", query.protocol());
        putQuery(values, "serviceName", query.serviceName());
        putQuery(values, "group", query.group());
        putQuery(values, "version", query.version());
        return values;
    }

    private void putQuery(Map<String, List<String>> query, String name, String value) {
        if (value != null && !value.isBlank()) {
            query.put(name, List.of(value));
        }
    }

    private String segment(String value, String fieldName) {
        return UriUtils.encodePathSegment(
                requireText(value, fieldName),
                StandardCharsets.UTF_8
        );
    }

    private static RestClient.Builder defaultBuilder(
            DdcManagementClientProperties properties
    ) {
        require(properties, "properties");
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        return RestClient.builder().requestFactory(requestFactory);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    private static <T> T require(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }
}
